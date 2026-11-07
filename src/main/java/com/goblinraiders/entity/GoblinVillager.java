package com.goblinraiders.entity;

import com.goblinraiders.registry.ModSounds;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.nbt.NbtUtils;
import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.entity.work.GoblinWorker;
import com.goblinraiders.entity.work.WorkGoal;
import com.goblinraiders.entity.work.WorkJob;
import com.goblinraiders.network.DialogChoicePayload;
import com.goblinraiders.registry.ModItems;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * A settler of a peaceful goblin village: a man or a woman with a trade - farmer, lumberjack, miner, builder or cook.
 * Each has a home of its own (where it was born or moved in; it sleeps there) and works at the job block of its trade
 * nearby - a composter, a fletching table, a grindstone, a stonecutter or a smoker - carrying what it gets to the
 * village storehouse. Settlers never fight anybody (hit one and it runs, the others just shout), work by day
 * ({@link WorkGoal}), sleep at night (a few stay up by the fire), chat with anybody and trade for goblin coins.
 */
public class GoblinVillager extends AbstractGoblin implements GoblinWorker, Merchant {
    private static final EntityDataAccessor<Boolean> DATA_FEMALE = SynchedEntityData.defineId(GoblinVillager.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DATA_JOB = SynchedEntityData.defineId(GoblinVillager.class, EntityDataSerializers.INT);
    private static final int WORK_RADIUS = 20;
    /** How far from home a settler looks for the job block of its trade. */
    public static final int SITE_RANGE = 48;
    /** How far from the workplace the storehouse may be. */
    private static final int STORE_RANGE = 64;
    private static final int COMPLAINT_TICKS = 900;
    private static final long RESTOCK_INTERVAL = 24000L;

    private final SimpleContainer pocket = new SimpleContainer(9);
    @Nullable
    private BlockPos workplace;
    /** Whether the template (or the builder) already decided who this is; otherwise it is rolled on spawn. */
    private boolean genderSet;
    private boolean jobSet;
    private long nextComplaint;
    @Nullable
    private Player tradingPlayer;
    @Nullable
    private MerchantOffers offers;
    private long lastRestock = -1;

    public GoblinVillager(EntityType<? extends GoblinVillager> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return goblinAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_FEMALE, false);
        builder.define(DATA_JOB, WorkJob.NONE.ordinal());
    }

    @Override
    protected void registerCombatGoals() {
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.35));
        this.goalSelector.addGoal(3, new WorkGoal(this));
    }

    // ---------------------------------------------------------------- who

    public boolean isFemale() {
        return this.entityData.get(DATA_FEMALE);
    }

    public void setFemale(boolean female) {
        this.entityData.set(DATA_FEMALE, female);
        this.genderSet = true;
    }

    @Override
    public WorkJob getJob() {
        return WorkJob.byId(this.entityData.get(DATA_JOB));
    }

    public void setJob(WorkJob job) {
        this.entityData.set(DATA_JOB, job.ordinal());
        this.jobSet = true;
        this.offers = null;
        this.setItemSlot(EquipmentSlot.MAINHAND, job.tool());
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    /** The trade of a settler: farmers are needed most (a trade without a job block nearby gets swapped later). */
    public static WorkJob newcomerJob(RandomSource random) {
        WorkJob[] jobs = {WorkJob.FARMER, WorkJob.FARMER, WorkJob.FARMER, WorkJob.LUMBERJACK, WorkJob.LUMBERJACK, WorkJob.MINER,
                WorkJob.BUILDER, WorkJob.BUILDER, WorkJob.COOK};
        return jobs[random.nextInt(jobs.length)];
    }

    /** The job block of each trade - vanilla job sites, so they are found quickly (points of interest). */
    @Nullable
    public static ResourceKey<PoiType> jobSite(WorkJob job) {
        return switch (job) {
            case FARMER -> PoiTypes.FARMER;          // composter
            case LUMBERJACK -> PoiTypes.FLETCHER;    // fletching table
            case MINER -> PoiTypes.WEAPONSMITH;      // grindstone (at the end of the mine tunnel)
            case BUILDER -> PoiTypes.MASON;          // stonecutter
            case COOK -> PoiTypes.BUTCHER;           // smoker
            case NONE -> null;
        };
    }

    /** "Goblin builder" / "goblin woman builder" instead of the plain type name. */
    @Override
    protected Component getTypeName() {
        return Component.translatable("entity.goblinraiders.goblin_villager." + this.getJob().key() + (this.isFemale() ? ".female" : ""));
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        if (!this.genderSet) {
            this.setFemale(level.getRandom().nextBoolean());
        }
        if (!this.jobSet) {
            this.setJob(newcomerJob(level.getRandom()));
        }
        return data;
    }

    @Override
    protected int homeRadius() {
        return SITE_RANGE + WORK_RADIUS;
    }

    public void setWorkplace(@Nullable BlockPos pos) {
        this.workplace = pos == null ? null : pos.immutable();
    }

    private boolean isJobSite(@Nullable BlockPos pos, WorkJob job) {
        ResourceKey<PoiType> site = jobSite(job);
        return pos != null && site != null && this.level() instanceof ServerLevel level
                && level.getPoiManager().getType(pos).map(type -> type.is(site)).orElse(false);
    }

    /** The nearest job block of {@code job} within {@link #SITE_RANGE} of home. */
    @Nullable
    public BlockPos findJobSite(WorkJob job) {
        ResourceKey<PoiType> site = jobSite(job);
        if (site == null || !(this.level() instanceof ServerLevel level)) {
            return null;
        }
        BlockPos from = this.getHome() != null ? this.getHome() : this.blockPosition();
        return level.getPoiManager().findClosest(type -> type.is(site), from, SITE_RANGE, PoiManager.Occupancy.ANY).orElse(null);
    }

    /**
     * Keeps the workplace on a job block of the trade: finds one when there is none (or it was broken), and when the
     * village has no job block for this trade at all, takes up a trade it does have.
     */
    public void checkWorkplace() {
        WorkJob job = this.getJob();
        if (job == WorkJob.NONE || this.isJobSite(this.workplace, job)) {
            return;
        }
        this.workplace = this.findJobSite(job);
        if (this.workplace != null) {
            return;
        }
        List<WorkJob> others = new ArrayList<>(List.of(WorkJob.FARMER, WorkJob.LUMBERJACK, WorkJob.MINER, WorkJob.BUILDER, WorkJob.COOK));
        Collections.shuffle(others, new java.util.Random(this.getUUID().getLeastSignificantBits()));
        for (WorkJob other : others) {
            BlockPos site = this.findJobSite(other);
            if (site != null) {
                this.setJob(other);
                this.workplace = site;
                return;
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if ((this.tickCount + this.getId()) % 200 == 0) {
            this.checkWorkplace();
        }
    }

    // ---------------------------------------------------------------- never a fight

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // Settlers do not fight.
    }

    @Override
    public boolean wantsToAttack(Player player) {
        return false;
    }

    @Override
    protected boolean fightsBack() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && source.getEntity() instanceof LivingEntity && !this.isSpeaking()) {
            this.sayRandom(Speech.HELP);
            this.stopTrading();
        }
        return hurt;
    }

    /** Most settlers sleep at night; the rest sit up by the fire (and keep it burning). */
    @Override
    protected int sleepersPercent() {
        return 85;
    }

    /** Nobody here raises an alarm about a guest. */
    @Override
    public void raiseAlarm(Player intruder) {
    }

    @Override
    public boolean isFamiliar(Player player) {
        return true;
    }

    @Override
    public boolean isPreventingPlayerRest(Player player) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    // ---------------------------------------------------------------- work

    @Override
    public AbstractGoblin asGoblin() {
        return this;
    }

    @Override
    @Nullable
    public BlockPos getWorkplace() {
        return this.workplace;
    }

    @Override
    public int workRadius() {
        return this.getJob() == WorkJob.LUMBERJACK ? 32 : WORK_RADIUS;
    }

    @Override
    public int storeRange() {
        return STORE_RANGE;
    }

    @Override
    public SimpleContainer getPocket() {
        return this.pocket;
    }

    @Override
    public boolean isWorking() {
        return this.workplace != null && this.getJob() != WorkJob.NONE && this.tradingPlayer == null;
    }

    @Override
    public void complain(String topic) {
        long now = this.level().getGameTime();
        if (now >= this.nextComplaint) {
            this.nextComplaint = now + COMPLAINT_TICKS;
            this.sayRandom(topic);
        }
    }

    // ---------------------------------------------------------------- talking and trading

    @Override
    public boolean talksWith(Player player) {
        return true;
    }

    @Override
    protected void onTalkStarted(ServerPlayer player) {
        GoblinAdvancements.award(player, GoblinAdvancements.SETTLER_TALK);
    }

    /** The trade's voice ("farmer", ...); lines it lacks come from the settlers' common ones. */
    @Override
    public String voice() {
        return this.getJob() == WorkJob.NONE ? "villager" : this.getJob().key();
    }

    @Override
    protected float pitchFactor() {
        return this.isFemale() ? 1.25F : 1.0F;
    }

    @Override
    public void onDialogChoice(ServerPlayer player, int choice) {
        if (choice == DialogChoicePayload.ACCEPT) {
            if (this.tradingPlayer == null || this.tradingPlayer == player) {
                this.restockIfNeeded();
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), 1);
            }
            return;
        }
        super.onDialogChoice(player, choice);
    }

    /** What each trade sells for goblin coins, and what it buys. */
    public static MerchantOffers offersFor(WorkJob job) {
        MerchantOffers offers = new MerchantOffers();
        Item coin = ModItems.GOBLIN_COIN.get();
        switch (job) {
            case FARMER -> {
                offers.add(sell(coin, 1, Items.CARROT, 8, 12));
                offers.add(sell(coin, 1, Items.POTATO, 8, 12));
                offers.add(sell(coin, 1, Items.WHEAT, 10, 12));
                offers.add(sell(coin, 2, Items.PUMPKIN, 2, 6));
                offers.add(sell(Items.BONE_MEAL, 12, coin, 1, 12));
            }
            case LUMBERJACK -> {
                offers.add(sell(coin, 2, Items.OAK_LOG, 12, 12));
                offers.add(sell(coin, 2, Items.SPRUCE_LOG, 12, 12));
                offers.add(sell(coin, 1, Items.OAK_SAPLING, 4, 8));
                offers.add(sell(coin, 1, Items.APPLE, 4, 8));
                offers.add(sell(Items.IRON_INGOT, 2, coin, 1, 10));
            }
            case MINER -> {
                offers.add(sell(coin, 1, Items.COAL, 12, 12));
                offers.add(sell(coin, 3, Items.RAW_IRON, 6, 8));
                offers.add(sell(coin, 4, Items.RAW_GOLD, 4, 6));
                offers.add(sell(coin, 9, Items.DIAMOND, 1, 3));
                offers.add(sell(Items.TORCH, 16, coin, 1, 10));
            }
            case BUILDER -> {
                offers.add(sell(coin, 1, Items.COBBLESTONE, 32, 12));
                offers.add(sell(coin, 2, Items.STONE_BRICKS, 24, 8));
                offers.add(sell(coin, 2, Items.LANTERN, 3, 8));
                offers.add(sell(coin, 1, ModItems.BUILDING_SITE.get(), 1, 4));
                offers.add(sell(Items.GLASS, 16, coin, 1, 10));
            }
            case COOK -> {
                offers.add(sell(coin, 1, ModItems.GOBLIN_GRUB.get(), 3, 16));
                offers.add(sell(coin, 1, Items.BAKED_POTATO, 6, 12));
                offers.add(sell(coin, 1, Items.BREAD, 4, 12));
                offers.add(sell(coin, 2, Items.PUMPKIN_PIE, 2, 6));
                offers.add(sell(Items.ROTTEN_FLESH, 12, coin, 1, 12));
            }
            default -> offers.add(sell(Items.EMERALD, 1, coin, 1, 8));
        }
        return offers;
    }

    private static MerchantOffer sell(ItemLike cost, int costCount, ItemLike result, int resultCount, int maxUses) {
        return new MerchantOffer(new ItemCost(cost, costCount), new ItemStack(result, resultCount), maxUses, 0, 0.0F);
    }

    private void restockIfNeeded() {
        long now = this.level().getGameTime();
        if (this.lastRestock < 0 || now - this.lastRestock >= RESTOCK_INTERVAL) {
            this.getOffers().forEach(MerchantOffer::resetUses);
            this.lastRestock = now;
        }
    }

    private void stopTrading() {
        if (this.tradingPlayer != null) {
            Player player = this.tradingPlayer;
            this.setTradingPlayer(null);
            player.closeContainer();
        }
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
        if (player != null) {
            this.startTalking(player, Integer.MAX_VALUE);
        } else {
            this.stopTalking();
        }
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = offersFor(this.getJob());
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        offer.increaseUses();
        if (this.tradingPlayer != null) {
            GoblinAdvancements.award(this.tradingPlayer, GoblinAdvancements.SETTLER_TRADE);
        }
        this.ambientSoundTime = -this.getAmbientSoundInterval();
        if (this.random.nextInt(3) == 0) {
            this.sayRandom("thanks");
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return ModSounds.GOBLIN_YES.get();
    }

    @Override
    public boolean canRestock() {
        return true;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide;
    }

    // ---------------------------------------------------------------- saving

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Female", this.isFemale());
        if (this.workplace != null) {
            tag.put("Workplace", NbtUtils.writeBlockPos(this.workplace));
        }
        tag.putInt("Job", this.getJob().ordinal());
        tag.put("Pocket", this.pocket.createTag(this.registryAccess()));
        tag.putLong("LastRestock", this.lastRestock);
        if (this.offers != null) {
            MerchantOffers.CODEC.encodeStart(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), this.offers)
                    .result().ifPresent(offersTag -> tag.put("Offers", offersTag));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Female")) {
            this.setFemale(tag.getBoolean("Female"));
        }
        if (tag.contains("Job")) {
            this.setJob(WorkJob.byId(tag.getInt("Job")));
        }
        this.pocket.fromTag(tag.getList("Pocket", Tag.TAG_COMPOUND), this.registryAccess());
        this.workplace = NbtUtils.readBlockPos(tag, "Workplace").orElse(null);
        this.lastRestock = tag.contains("LastRestock") ? tag.getLong("LastRestock") : -1;
        if (tag.contains("Offers")) {
            Optional<MerchantOffers> loaded = MerchantOffers.CODEC.parse(this.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                    tag.get("Offers")).result();
            loaded.ifPresent(o -> this.offers = o);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        Containers.dropContents(level, this, this.pocket);
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        this.stopTrading();
    }
}
