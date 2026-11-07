package com.goblinraiders.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.ChatFormatting;
import com.goblinraiders.registry.ModItems;
import com.goblinraiders.raid.GoblinRaids;
import com.goblinraiders.network.OpenDialogPayload;
import com.goblinraiders.network.ModNetwork;
import com.goblinraiders.network.DialogChoicePayload;
import com.goblinraiders.block.GoblinStashBlockEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import java.util.Objects;
import com.goblinraiders.entity.goal.WatchGoal;
import com.goblinraiders.entity.goal.SleepGoal;
import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.entity.goal.GoblinChatGoal;
import com.goblinraiders.entity.goal.CaravanGoal;
import com.goblinraiders.entity.goal.FollowHirerGoal;
import com.goblinraiders.entity.goal.GoblinTalkGoal;
import com.goblinraiders.entity.goal.HirerDefendGoal;
import com.goblinraiders.raid.Caravans;
import com.goblinraiders.raid.Horde;
import com.goblinraiders.item.GoblinClubItem;
import com.goblinraiders.registry.ModSounds;
import com.goblinraiders.registry.ModTags;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Shared behaviour of the goblin race: they live in villages, stick around their home, stand up for each other,
 * mutter to themselves, chat with each other and shout at the players they fight. Speech shows as a bubble above
 * the head (synced key of {@link Speech}); lines aimed at a player also go to that player's chat.
 */
public abstract class AbstractGoblin extends Monster {
    /** Goblins wander at most this far from the village they belong to. */
    public static final int HOME_RADIUS = 28;
    /** Natural spawning stops once this many goblins are around, so villages do not overflow. */
    private static final int MAX_GOBLINS_NEARBY = 16;
    /** How long a speech bubble stays, in ticks. */
    public static final int SPEECH_TICKS = 70;
    /** How long one snore lasts. */
    private static final int SNORE_TICKS = 45;
    /** How long a goblin startled awake stays up and alert. */
    private static final long STARTLED_TICKS = 1200;
    /** A player gets at most one goblin line in chat per this many ticks (from all goblins together). */
    private static final int PLAYER_CHAT_COOLDOWN = 400;
    private static final String LAST_CHAT_KEY = "goblinraiders.last_goblin_chat";

    private static final EntityDataAccessor<String> DATA_SPEECH = SynchedEntityData.defineId(AbstractGoblin.class, EntityDataSerializers.STRING);

    @Nullable
    private BlockPos home;
    private int speechTicks;
    /** A sleeper's next breath, and when it last snored (neighbours wait their turn). */
    private int nextSnore;
    private long lastSnore = Long.MIN_VALUE / 2;
    /** Client: the tick the current line began (the face moves its mouth for a while after). */
    private int speechStartedAt;
    /** Set on goblins of a raid caravan (see {@link com.goblinraiders.raid.Caravans}); such goblins are never saved. */
    @Nullable
    private UUID caravanId;
    /** Which share of the caravan's loot this goblin carries, or -1 for guards. */
    private int caravanShare = -1;
    /** The player who hired this goblin through the king's horde, and until when. */
    @Nullable
    private UUID hirer;
    private long hiredUntil;
    /** The goblin this one is having a conversation with (see {@link GoblinChatGoal}). */
    @Nullable
    public AbstractGoblin chatPartner;
    /** A player this goblin is talking to through a dialog window; it stands still and looks at them. */
    @Nullable
    private Player talkingTo;
    private int talkTicks;
    /** A goblin startled awake (or alarmed) stays up and alert until then, even at night. */
    private long stayAwakeUntil;
    private long nextAlarm;

    protected AbstractGoblin(EntityType<? extends AbstractGoblin> type, Level level) {
        super(type, level);
        if (this.getNavigation() instanceof GroundPathNavigation navigation) {
            navigation.setCanOpenDoors(true);
        }
    }

    protected static AttributeSupplier.Builder goblinAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPEECH, "");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Loot carriers of a caravan run from players instead of fighting.
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 10.0F, 1.0, 1.35,
                player -> this.isCarryingLoot() && !GoblinFriends.isFriend(player)));
        this.goalSelector.addGoal(1, new GoblinTalkGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(1, new SleepGoal(this));
        this.registerCombatGoals();
        this.goalSelector.addGoal(3, new CaravanGoal(this));
        this.goalSelector.addGoal(3, new FollowHirerGoal(this));
        this.goalSelector.addGoal(4, new WatchGoal(this));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.9));
        this.goalSelector.addGoal(5, new GoblinChatGoal(this));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, AbstractGoblin.class, 6.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        if (this.fightsBack()) {
            this.targetSelector.addGoal(0, new HirerDefendGoal(this));
            this.targetSelector.addGoal(1, new GoblinHurtByTargetGoal(this));
        }
    }

    /** Attack goals (priority 2-3) and, for hostile goblins, target goals (priority 2+). */
    protected abstract void registerCombatGoals();

    /** False for goblins that never fight; they only call for help. */
    protected boolean fightsBack() {
        return true;
    }

    protected void addHostileTargets() {
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false,
                target -> target instanceof Player player && this.wantsToAttack(player)));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillager.class, 10, false, false, t -> !this.isHired()));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, IronGolem.class, 10, true, false, t -> !this.isHired()));
    }

    /** Whether this goblin would start a fight with the player unprovoked. */
    public boolean wantsToAttack(Player player) {
        return !this.isHired() && !this.isCarryingLoot() && !GoblinFriends.isFriend(player) && this.notices(player);
    }

    /** At night a sneaking player gets close unnoticed - unless the goblin is on alert. A sleeping goblin notices nobody. */
    public boolean notices(Player player) {
        if (this.isDozing()) {
            return false;
        }
        double sneakRange = GoblinNight.SPOT_RANGE_SNEAKING;
        return !(GoblinNight.isNight(this.level()) && player.isCrouching() && !this.isAlert()
                && this.distanceToSqr(player) > sneakRange * sneakRange);
    }

    /** Whether this goblin keeps fighting the player even through a disguise: the player struck it a moment ago. */
    public boolean holdsGrudge(Player player) {
        return this.getLastHurtByMob() == player && this.tickCount - this.getLastHurtByMobTimestamp() < 200;
    }

    // ---------------------------------------------------------------- small talk

    private static final int TALK_TICKS = 20 * 60;
    /** A goblin takes one treat a day. */
    private long lastTreatDay = -1;

    /** Whether this goblin chats with {@code player} in the talk window: those that take the player for one of their own do. */
    public boolean talksWith(Player player) {
        return GoblinFriends.isFriend(player);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || player.getItemInHand(hand).is(Items.NAME_TAG) || !this.isAlive() || this.isHired()) {
            return super.mobInteract(player, hand);
        }
        if (this.wakeIfDozing()) {
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (!this.talksWith(player)) {
            return super.mobInteract(player, hand);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (this.getTarget() != null) {
                this.sayRandom(Speech.TAUNT);   // busy fighting
            } else {
                this.openTalk(serverPlayer);
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    protected void openTalk(ServerPlayer player) {
        this.getNavigation().stop();
        this.startTalking(player, TALK_TICKS);
        ModNetwork.send(player, new OpenDialogPayload(this.getId(), new MerchantOffers()));
        this.playSound(this.getTalkSound(), this.getSoundVolume(), this.getVoicePitch());
        this.onTalkStarted(player);
    }

    /** Advancement for a conversation: by default, talking to a goblin that took you for one of its own. */
    protected void onTalkStarted(ServerPlayer player) {
        GoblinAdvancements.award(player, GoblinAdvancements.UNDERCOVER);
    }

    /** The player's answer from a dialog window. */
    public void onDialogChoice(ServerPlayer player, int choice) {
        if (choice == DialogChoicePayload.NEUTRAL_TREAT) {
            this.takeTreat(player);
        } else if (choice == DialogChoicePayload.CLOSE && this.talkingTo == player) {
            this.stopTalking();
        }
    }

    /** Eats a bowl of grub from the player (once a day) and tells where the village stash is. */
    public boolean takeTreat(ServerPlayer player) {
        long day = this.level().getDayTime() / 24000L;
        if (day == this.lastTreatDay || !takeGrub(player)) {
            this.playSound(ModSounds.GOBLIN_NO.get(), this.getSoundVolume(), this.getVoicePitch());
            return false;
        }
        this.lastTreatDay = day;
        this.playSound(SoundEvents.GENERIC_EAT, 1.0F, this.getVoicePitch());
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getEyeY() + 0.3, this.getZ(), 8, 0.3, 0.3, 0.3, 0.0);
        }
        BlockPos stash = this.findStash();
        Component tip = stash == null
                ? Component.translatable("goblinraiders.dialog.neutral.no_stash")
                : Component.translatable("goblinraiders.dialog.neutral.stash", (int) Math.sqrt(stash.distSqr(this.blockPosition())),
                        Component.translatable("goblinraiders.direction." + GoblinRaids.compass(this.blockPosition(), stash)));
        player.sendSystemMessage(Component.translatable("goblinraiders.speech.format", this.getDisplayName(), tip).withStyle(ChatFormatting.GRAY));
        GoblinAdvancements.award(player, GoblinAdvancements.TREAT);
        return true;
    }

    private static boolean takeGrub(Player player) {
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.GOBLIN_GRUB.get())) {
                stack.shrink(1);
                if (!player.getInventory().add(new ItemStack(Items.BOWL))) {
                    player.drop(new ItemStack(Items.BOWL), false);
                }
                return true;
            }
        }
        return false;
    }

    /** The stash of the village this goblin lives in (searched in the loaded chunks around its home). */
    @Nullable
    private BlockPos findStash() {
        BlockPos center = this.home != null ? this.home : this.blockPosition();
        ChunkPos chunk = new ChunkPos(center);
        BlockPos best = null;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (!this.level().hasChunk(chunk.x + dx, chunk.z + dz)) {
                    continue;
                }
                for (BlockEntity entity : this.level().getChunk(chunk.x + dx, chunk.z + dz).getBlockEntities().values()) {
                    if (entity instanceof GoblinStashBlockEntity
                            && (best == null || entity.getBlockPos().distSqr(center) < best.distSqr(center))) {
                        best = entity.getBlockPos();
                    }
                }
            }
        }
        return best;
    }

    // ---------------------------------------------------------------- night

    /** Out of a hundred goblins of this kind, how many sleep at night; the rest keep watch. */
    protected int sleepersPercent() {
        return 65;
    }

    /** Whether this kind stands watch at night (the elder and the king do not). */
    protected boolean keepsWatch() {
        return true;
    }

    /** Village goblins only: mercenaries and caravan goblins have no home to sleep in. */
    private boolean livesInVillage() {
        return this.home != null && this.hirer == null && this.caravanId == null;
    }

    /** Whether this goblin sleeps at night - fixed for each goblin (from its UUID). */
    public boolean isSleeper() {
        return this.livesInVillage() && Math.floorMod(this.getUUID().hashCode(), 100) < this.sleepersPercent();
    }

    public boolean isWatchman() {
        return this.livesInVillage() && !this.isSleeper() && this.keepsWatch();
    }

    public boolean isDozing() {
        return this.hasPose(Pose.SLEEPING);
    }

    /** Recently startled or alarmed: sneaking does not help against it, and it does not go back to sleep yet. */
    public boolean isAlert() {
        return this.level().getGameTime() < this.stayAwakeUntil;
    }

    public boolean canDozeOff() {
        return this.isSleeper() && this.isAlive() && GoblinNight.isNight(this.level()) && !this.isAlert() && this.getTarget() == null
                && this.talkingTo == null && !this.isInWater() && !this.isPassenger();
    }

    public void fallAsleep() {
        if (this.isDozing()) {
            return;
        }
        this.getNavigation().stop();
        this.setTarget(null);
        this.setPose(Pose.SLEEPING);
        this.targetSelector.disableControlFlag(Goal.Flag.TARGET);   // asleep, it notices nobody
    }

    /** Wakes up; a goblin {@code startled} awake grumbles and stays up and alert for a minute. */
    public void wakeUp(boolean startled) {
        if (startled) {
            this.stayAwakeUntil = Math.max(this.stayAwakeUntil, this.level().getGameTime() + STARTLED_TICKS);
        }
        if (!this.isDozing()) {
            return;
        }
        this.setPose(Pose.STANDING);
        this.targetSelector.enableControlFlag(Goal.Flag.TARGET);
        if (startled) {
            this.sayRandom(Speech.WOKEN);
        }
    }

    /** For mobInteract: clicking a dozing goblin wakes it (grumbling) instead of starting a conversation. */
    protected boolean wakeIfDozing() {
        if (!this.isDozing()) {
            return false;
        }
        this.wakeUp(true);
        return true;
    }

    /** An intruder was spotted: a shout, and the goblins around wake up and stay alert. */
    public void raiseAlarm(Player intruder) {
        long now = this.level().getGameTime();
        if (now < this.nextAlarm) {
            return;
        }
        this.nextAlarm = now + 200;
        this.shoutRandom(Speech.ALARM, List.of(intruder));
        this.playSound(this.getAngrySound(), this.getSoundVolume() * 1.5F, this.getVoicePitch());
        this.playSound(ModSounds.WAR_HORN.get(), 4.0F, 1.0F);   // the whole village hears it
        this.wakeNeighbours(intruder);
    }

    private void wakeNeighbours(Player intruder) {
        double range = GoblinNight.ALARM_RANGE;
        for (AbstractGoblin goblin : this.level().getEntitiesOfClass(AbstractGoblin.class, this.getBoundingBox().inflate(range, 8.0, range),
                g -> g.isAlive() && !g.isHired())) {
            goblin.wakeUp(true);
        }
        GoblinAdvancements.award(intruder, GoblinAdvancements.SPOTTED);
    }

    /** A sleeping goblin lies flat. */
    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        if (pose == Pose.SLEEPING) {
            EntityDimensions standing = super.getDefaultDimensions(Pose.STANDING);
            float height = Math.min(standing.height(), 0.6F);
            return EntityDimensions.scalable(standing.width(), height).withEyeHeight(height * 0.5F);
        }
        return super.getDefaultDimensions(pose);
    }

    // ---------------------------------------------------------------- home

    public void setHome(BlockPos pos) {
        this.home = pos.immutable();
        this.restrictTo(this.home, this.homeRadius());
    }

    protected void clearHome() {
        this.home = null;
        this.clearRestriction();
    }

    /** The player this goblin follows around: its employer (children: their owner). */
    @Nullable
    public Player getLeader() {
        return this.getHirer();
    }

    /** Whether this goblin lights the village campfires at dusk and puts them out in the morning. */
    protected boolean tendsFires() {
        return true;
    }

    /** A player this goblin knows well enough not to sound the alarm about at night. */
    public boolean isFamiliar(Player player) {
        return false;
    }

    /** How far from home this goblin wanders. */
    protected int homeRadius() {
        return HOME_RADIUS;
    }

    @Nullable
    public BlockPos getHome() {
        return this.home;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.home != null) {
            tag.put("Home", NbtUtils.writeBlockPos(this.home));
        }
        if (this.hirer != null) {
            tag.putUUID("Hirer", this.hirer);
            tag.putLong("HiredUntil", this.hiredUntil);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Home").ifPresent(this::setHome);
        if (tag.hasUUID("Hirer")) {
            this.hireBy(tag.getUUID("Hirer"), tag.getLong("HiredUntil"));
        }
    }

    // ---------------------------------------------------------------- caravans and mercenaries

    public void joinCaravan(UUID caravan, int share) {
        this.caravanId = caravan;
        this.caravanShare = share;
        this.home = null;
        this.clearRestriction();
    }

    @Nullable
    public UUID getCaravanId() {
        return this.caravanId;
    }

    public int getCaravanShare() {
        return this.caravanShare;
    }

    public boolean isCarryingLoot() {
        return this.caravanId != null && this.caravanShare >= 0;
    }

    public void hireBy(UUID player, long until) {
        this.hirer = player;
        this.hiredUntil = until;
        this.home = null;
        this.clearRestriction();
        this.setPersistenceRequired();
    }

    public boolean isHired() {
        return this.hirer != null;
    }

    @Nullable
    public Player getHirer() {
        return this.hirer == null ? null : this.level().getPlayerByUUID(this.hirer);
    }

    /** Caravan goblins live in the caravan record, not in the world save (that is what keeps the loot from doubling). */
    @Override
    public boolean shouldBeSaved() {
        return this.caravanId == null && super.shouldBeSaved();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return this.caravanId == null && this.hirer == null && super.removeWhenFarAway(distanceToClosestPlayer);
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (!this.level().isClientSide && this.isCarryingLoot()) {
            Caravans.onCarrierDeath(this, damageSource);
        }
    }

    // ---------------------------------------------------------------- spawning

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.populateDefaultEquipmentSlots(level.getRandom(), difficulty);
        if (spawnType == MobSpawnType.STRUCTURE) {
            // Placed together with the village: they live there and must not despawn.
            this.setPersistenceRequired();
            this.setHome(this.blockPosition());
        } else if (spawnType == MobSpawnType.NATURAL
                && level.getLevel().structureManager().getStructureWithPieceAt(this.blockPosition(), ModTags.GOBLIN_VILLAGES).isValid()) {
            this.setHome(this.blockPosition());
        }
        return data;
    }

    public static boolean checkGoblinSpawnRules(EntityType<? extends AbstractGoblin> type, ServerLevelAccessor level,
                                                MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || !Mob.checkMobSpawnRules(type, level, spawnType, pos, random)
                || !canWalkOut(type, level, pos)) {
            return false;
        }
        if (spawnType == MobSpawnType.NATURAL || spawnType == MobSpawnType.CHUNK_GENERATION) {
            return level.getEntitiesOfClass(AbstractGoblin.class, new AABB(pos).inflate(32.0, 16.0, 32.0)).size() < MAX_GOBLINS_NEARBY;
        }
        return true;
    }

    /**
     * Goblins taller than a door (the giant, the king) only appear under the open sky: in a hall they would never get
     * out of it.
     */
    public static boolean canWalkOut(EntityType<?> type, LevelReader level, BlockPos pos) {
        return type.getHeight() <= 2.0F || pos.getY() >= level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
    }

    /** Goblins do not care about light, unlike regular monsters that prefer darkness. */
    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return 0.0F;
    }

    // ---------------------------------------------------------------- relations

    /** Village goblins stand together; a mercenary sides with its employer and the employer's other mercenaries. */
    @Override
    public boolean isAlliedTo(Entity other) {
        if (other instanceof AbstractGoblin goblin) {
            return Objects.equals(this.hirer, goblin.hirer);
        }
        return this.hirer != null && this.hirer.equals(other.getUUID()) || super.isAlliedTo(other);
    }

    /** Makes the goblins around go after {@code attacker}. */
    public void callForHelp(LivingEntity attacker, double range) {
        AABB area = this.getBoundingBox().inflate(range, 8.0, range);
        for (AbstractGoblin goblin : this.level().getEntitiesOfClass(AbstractGoblin.class, area, EntitySelector.NO_SPECTATORS)) {
            if (goblin != this && goblin.fightsBack() && goblin.getTarget() == null && !goblin.isAlliedTo(attacker)) {
                goblin.setTarget(attacker);
            }
        }
    }

    // ---------------------------------------------------------------- talking

    /** Voice of this goblin in {@link Speech} keys: "small", "ruthless", ... */
    public abstract String voice();

    /** The translation key of what the goblin is saying right now, or "". */
    public String getSpeech() {
        return this.entityData.get(DATA_SPEECH);
    }

    public boolean isSpeaking() {
        return !this.getSpeech().isEmpty();
    }

    /** Client: ticks since the current line appeared. */
    public int speechAge() {
        return this.tickCount - this.speechStartedAt;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_SPEECH.equals(key)) {
            this.speechStartedAt = this.tickCount;
        }
    }

    /** Shows a speech bubble with the given line. */
    public void say(String key) {
        this.showSpeech(key);
        this.playSound(this.getTalkSound(), this.getSoundVolume(), this.getVoicePitch());
    }

    private void showSpeech(String key) {
        this.entityData.set(DATA_SPEECH, key);
        this.speechTicks = SPEECH_TICKS;
    }

    /** A bubble without the voice sound (sleep talk). */
    public void mumble(String topic) {
        Speech.random(this.voice(), topic, this.random).ifPresent(this::showSpeech);
    }

    /**
     * A bubble plus a chat line "<goblin> line" for the given players - but each player gets at most one goblin line
     * per {@link #PLAYER_CHAT_COOLDOWN} ticks, however many goblins are shouting, so fights do not flood the chat.
     */
    public void shout(String key, Collection<? extends Player> to) {
        this.say(key);
        long now = this.level().getGameTime();
        Component line = Component.translatable("goblinraiders.speech.format", this.getDisplayName(), Component.translatable(key));
        for (Player player : to) {
            if (now - player.getPersistentData().getLong(LAST_CHAT_KEY) >= PLAYER_CHAT_COOLDOWN) {
                player.getPersistentData().putLong(LAST_CHAT_KEY, now);
                player.sendSystemMessage(line);
            }
        }
    }

    public void sayRandom(String topic) {
        Speech.random(this.voice(), topic, this.random).ifPresent(this::say);
    }

    public void shoutRandom(String topic, Collection<? extends Player> to) {
        Speech.random(this.voice(), topic, this.random).ifPresent(key -> this.shout(key, to));
    }

    /** Players within {@code range} blocks, for lines everyone around should hear. */
    public List<Player> playersAround(double range) {
        return this.level().getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(range), EntitySelector.NO_SPECTATORS);
    }

    public boolean canChat() {
        return this.isAlive() && !this.isDozing() && this.getTarget() == null && this.talkingTo == null && this.chatPartner == null
                && !this.isSpeaking();
    }

    public void startTalking(Player player, int ticks) {
        this.talkingTo = player;
        this.talkTicks = ticks;
    }

    public void stopTalking() {
        this.talkingTo = null;
        this.talkTicks = 0;
    }

    @Nullable
    public Player getTalkingTo() {
        return this.talkingTo;
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && (this.isCarryingLoot() || this.isHired() && this.isAlliedTo(target))) {
            return;   // loot carriers flee instead, mercenaries never turn on their employer or each other
        }
        if (target != null && this.isDozing()) {
            this.wakeUp(true);
        }
        LivingEntity previous = this.getTarget();
        super.setTarget(target);
        if (!this.level().isClientSide && target != previous && target instanceof Player player && this.isAlive()
                && !player.isCreative() && !player.isSpectator()) {
            this.onStartAttacking(player);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        SoundEvent grunt = this.getAttackSound();
        if (hit && grunt != null) {
            this.playSound(grunt, this.getSoundVolume() * 0.8F, this.getVoicePitch());
        }
        if (hit && target instanceof LivingEntity living && this.getMainHandItem().getItem() instanceof GoblinClubItem) {
            GoblinClubItem.onHit(living);
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            this.wakeUp(true);
            if (source.getEntity() instanceof Player player && !this.isAlliedTo(player)) {
                GoblinFriends.betray(player);   // striking a goblin ends any disguise (one's own mercenary does not count)
            }
        }
        return hurt;
    }

    /** Battle cry when a fight with a player starts. */
    protected void onStartAttacking(Player player) {
        this.shoutRandom(Speech.BATTLE, List.of(player));
        this.playSound(this.getAngrySound(), this.getSoundVolume(), this.getVoicePitch());
        if (this.home != null && GoblinNight.isNight(this.level())) {
            this.wakeNeighbours(player);   // a fight at night wakes the village
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            return;
        }
        if (this.speechTicks > 0 && --this.speechTicks == 0) {
            this.entityData.set(DATA_SPEECH, "");
        }
        if (this.isDozing()) {
            this.breathe();
        }
        if (this.isCarryingLoot() && (this.tickCount + this.getId()) % 25 == 0 && this.getDeltaMovement().horizontalDistanceSqr() > 1.0E-3) {
            this.playSound(ModSounds.LOOT_JINGLE.get(), 0.6F, 0.9F + this.random.nextFloat() * 0.2F);
        }
        if (this.hirer != null && this.level().getGameTime() > this.hiredUntil) {
            Horde.dismiss(this);
            return;
        }
        if (this.talkingTo != null && (--this.talkTicks <= 0 || !this.talkingTo.isAlive() || this.distanceToSqr(this.talkingTo) > 100.0)) {
            this.stopTalking();
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        // Whoever has someone to fight looks angry (melee goals say so themselves, potion throwers and archers not always).
        boolean fighting = this.getTarget() != null && this.getTarget().isAlive();
        if (fighting != this.isAggressive()) {
            this.setAggressive(fighting);
        }
        if (this.isDozing()) {
            return;
        }
        if (this.home != null && this.hirer == null && this.tendsFires() && (this.tickCount + this.getId()) % 100 == 0) {
            GoblinNight.tendFire(this, GoblinNight.isNight(this.level()));
        }
        if (this.getTarget() instanceof Player player) {
            if (this.random.nextInt(260) == 0) {
                this.sayRandom(Speech.TAUNT);   // taunts are bubbles only; the chat gets the battle cry
            }
        } else if (!this.isSpeaking() && this.chatPartner == null && this.talkingTo == null && this.random.nextInt(this.idleLineChance()) == 0) {
            this.sayRandom(Speech.IDLE);
        }
    }

    /** 1 in this many ticks the goblin mutters something to itself. */
    protected int idleLineChance() {
        return 1400;
    }

    // ---------------------------------------------------------------- sounds

    /** Multiplier on top of the random voice pitch: small goblins squeak, giants rumble. */
    protected abstract float pitchFactor();

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * this.pitchFactor();
    }

    protected SoundEvent getTalkSound() {
        return ModSounds.GOBLIN_TALK.get();
    }

    protected SoundEvent getAngrySound() {
        return ModSounds.GOBLIN_ANGRY.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.random.nextInt(4) == 0 ? ModSounds.GOBLIN_LAUGH.get() : ModSounds.GOBLIN_AMBIENT.get();
    }

    /** A grunt with every blow that lands, or null when the goblin has its own (giants and the king). */
    @Nullable
    protected SoundEvent getAttackSound() {
        return ModSounds.GOBLIN_ATTACK.get();
    }

    /** Gloating over a beaten player. */
    protected SoundEvent getCelebrateSound() {
        return ModSounds.GOBLIN_CELEBRATE.get();
    }

    /** Sleepers make no other noise: they snore with their breathing (see {@link #breathe}). */
    @Override
    public void playAmbientSound() {
        if (!this.isDozing()) {
            super.playAmbientSound();
        }
    }

    /**
     * A sleeper snores at its own steady pace, every 3.5 to 5.5 seconds; sleepers side by side take turns instead of
     * snoring over each other, in voices only a little higher or lower than one another: snores laid over each other at
     * clashing pitches sounded wrong.
     */
    private void breathe() {
        if (this.tickCount < this.nextSnore) {
            return;
        }
        long now = this.level().getGameTime();
        long neighbourDone = this.level().getEntitiesOfClass(AbstractGoblin.class, this.getBoundingBox().inflate(8.0), g -> g != this)
                .stream().mapToLong(g -> g.lastSnore + SNORE_TICKS).max().orElse(Long.MIN_VALUE);
        if (now < neighbourDone) {
            // Someone nearby is snoring: take the next turn, just after it ends.
            this.nextSnore = this.tickCount + (int) (neighbourDone - now) + 5 + this.random.nextInt(15);
            return;
        }
        this.nextSnore = this.tickCount + 70 + Math.floorMod(this.getId() * 37, 30) + this.random.nextInt(10);
        this.lastSnore = now;
        float pitch = Mth.clamp(this.pitchFactor(), 0.85F, 1.12F) * (0.97F + this.random.nextFloat() * 0.06F);
        this.playSound(ModSounds.GOBLIN_SNORE.get(), this.getSoundVolume() * 0.7F, pitch);
    }

    /** A goblin that beat a player gloats, and the fighters around it cheer. */
    @Override
    public boolean killedEntity(ServerLevel level, LivingEntity victim) {
        boolean result = super.killedEntity(level, victim);
        if (victim instanceof Player) {
            this.playSound(this.getCelebrateSound(), this.getSoundVolume() * 1.5F, this.getVoicePitch());
            level.getEntitiesOfClass(AbstractGoblin.class, this.getBoundingBox().inflate(12.0),
                            g -> g != this && g.fightsBack() && !g.isDozing() && g.isAlive()).stream().limit(3)
                    .forEach(g -> g.playSound(g.getCelebrateSound(), g.getSoundVolume(), g.getVoicePitch()));
        }
        return result;
    }

    /** Fighters grunt when hurt; peaceful goblins squeal. */
    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return this.fightsBack() ? ModSounds.GOBLIN_HURT.get() : ModSounds.GOBLIN_SCARED.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GOBLIN_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(SoundEvents.PIGLIN_STEP, 0.15F, 1.0F);
    }

    /** Retaliates and calls every goblin around (not only the same kind) to help. */
    static class GoblinHurtByTargetGoal extends HurtByTargetGoal {
        GoblinHurtByTargetGoal(AbstractGoblin goblin) {
            super(goblin);   // allies are skipped by the targeting conditions; mercenaries are not allies
            this.setAlertOthers();
        }

        @Override
        protected void alertOthers() {
            LivingEntity attacker = this.mob.getLastHurtByMob();
            if (attacker != null) {
                ((AbstractGoblin) this.mob).callForHelp(attacker, this.getFollowDistance());
            }
        }
    }
}
