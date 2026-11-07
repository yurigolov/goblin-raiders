package com.goblinraiders.entity;

import com.goblinraiders.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.entity.goal.ChildScuffleGoal;
import com.goblinraiders.entity.work.GoblinWorker;
import com.goblinraiders.entity.work.WorkGoal;
import com.goblinraiders.entity.work.WorkJob;
import com.goblinraiders.entity.work.WorkSkills;
import com.goblinraiders.network.DialogChoicePayload;
import com.goblinraiders.network.ModNetwork;
import com.goblinraiders.network.OpenWorkerDialogPayload;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * A goblin child: one block tall, in a small goblin's or a giant's skin. Children never fight players or grown-ups -
 * not even when hit (the grown-ups around come to their aid instead) - but they scuffle with each other.
 * <p>
 * A child bought from the king ({@link com.goblinraiders.raid.Horde#buyWorker}) belongs to a player and works for them:
 * it follows its owner until told to work somewhere, then tends the fields around (harvests ripe crops, replants,
 * sows empty farmland) or digs at the rock face of a mine, and carries what it gets into the nearest chest.
 */
public class GoblinChild extends AbstractGoblin implements GoblinWorker {
    /** What keeps a worker from working, shown in its dialog. */
    public enum Problem {
        NONE, NO_CHEST, NOT_A_MINE, NO_FIELD, FULL
    }

    public static final int VARIANT_SMALL = 0;
    public static final int VARIANT_GIANT = 1;
    /** Workers keep within this distance of their workplace. */
    public static final int WORK_RADIUS = 8;
    private static final int TALK_TICKS = 20 * 60;
    private static final int COMPLAINT_TICKS = 600;
    private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(GoblinChild.class, EntityDataSerializers.INT);

    @Nullable
    private UUID owner;
    private WorkJob job = WorkJob.NONE;
    @Nullable
    private BlockPos workplace;
    private final SimpleContainer pocket = new SimpleContainer(9);
    private long nextComplaint;

    public GoblinChild(EntityType<? extends GoblinChild> type, Level level) {
        super(type, level);
        this.xpReward = 0;
        this.refreshDimensions();   // the type's size halved: a child is always a baby
    }

    public static AttributeSupplier.Builder createAttributes() {
        return goblinAttributes()
                .add(Attributes.MAX_HEALTH, 8.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_VARIANT, VARIANT_SMALL);
    }

    @Override
    protected void registerCombatGoals() {
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.4) {
            @Override
            protected boolean shouldPanic() {
                // Runs from grown-ups and players, but stands its ground against other children.
                return super.shouldPanic() && !(this.mob.getLastHurtByMob() instanceof GoblinChild);
            }
        });
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2, false));
        this.goalSelector.addGoal(3, new WorkGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return this.mob.getLastHurtByMob() instanceof GoblinChild && super.canUse();
            }
        });
        this.targetSelector.addGoal(2, new ChildScuffleGoal(this));
    }

    // ---------------------------------------------------------------- what a child is

    @Override
    public boolean isBaby() {
        return true;
    }

    public int getVariant() {
        return this.entityData.get(DATA_VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(DATA_VARIANT, Mth.clamp(variant, VARIANT_SMALL, VARIANT_GIANT));
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        this.setVariant(level.getRandom().nextInt(2));
        return super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
    }

    /** Never against players or grown-ups: only a scuffle with another child. */
    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target == null || target instanceof GoblinChild) {
            super.setTarget(target);
        }
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
    protected SoundEvent getAmbientSound() {
        return this.random.nextBoolean() ? ModSounds.CHILD_GIGGLE.get() : super.getAmbientSound();
    }

    /** A scuffle between children is a game. */
    @Override
    protected SoundEvent getAttackSound() {
        return ModSounds.CHILD_GIGGLE.get();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof GoblinChild)) {
            this.callForHelp(attacker, 16.0);   // grown-ups stand up for children
            if (!this.isSpeaking()) {
                this.sayRandom(Speech.HELP);
            }
        }
        return hurt;
    }

    @Override
    protected int sleepersPercent() {
        return 100;   // bedtime!
    }

    @Override
    protected boolean keepsWatch() {
        return false;
    }

    @Override
    protected boolean tendsFires() {
        return false;
    }

    @Override
    public boolean isFamiliar(Player player) {
        return player.getUUID().equals(this.owner);
    }

    @Override
    protected int homeRadius() {
        return this.owner != null ? WORK_RADIUS + 2 : 20;
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
        return this.owner == null && super.removeWhenFarAway(distanceToClosestPlayer);
    }

    @Override
    public String voice() {
        return "child";
    }

    @Override
    protected float pitchFactor() {
        return 0.9F;   // a baby is pitched up by half already (getVoicePitch)
    }

    @Override
    protected int idleLineChance() {
        return 900;
    }

    // ---------------------------------------------------------------- talking

    /** Children chat with anybody. */
    @Override
    public boolean talksWith(Player player) {
        return true;
    }

    @Override
    protected void onTalkStarted(ServerPlayer player) {
        GoblinAdvancements.award(player, GoblinAdvancements.CHILD_TALK);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!this.isOwner(player) || player.isSecondaryUseActive() || !this.isAlive()) {
            return super.mobInteract(player, hand);
        }
        if (!this.wakeIfDozing() && player instanceof ServerPlayer serverPlayer) {
            this.startTalking(player, TALK_TICKS);
            this.openWorkerDialog(serverPlayer);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    private void openWorkerDialog(ServerPlayer player) {
        ModNetwork.send(player, new OpenWorkerDialogPayload(this.getId(), this.job.ordinal(), this.workplace != null,
                this.problem().ordinal(), this.itemsCarried()));
    }

    @Override
    public void onDialogChoice(ServerPlayer player, int choice) {
        if (!this.isOwner(player) || choice == DialogChoicePayload.NEUTRAL_TREAT) {
            super.onDialogChoice(player, choice);
            return;
        }
        switch (choice) {
            case DialogChoicePayload.WORKER_STAY -> {
                this.setWorkplace(this.blockPosition());
                this.sayRandom("work");
            }
            case DialogChoicePayload.WORKER_FOLLOW -> this.setWorkplace(null);
            case DialogChoicePayload.WORKER_FARM -> this.setJob(WorkJob.FARMER);
            case DialogChoicePayload.WORKER_MINE -> this.setJob(WorkJob.MINER);
            case DialogChoicePayload.WORKER_HAND_OVER -> this.handOver(player);
            default -> {
                if (this.getTalkingTo() == player) {
                    this.stopTalking();
                }
                return;
            }
        }
        this.openWorkerDialog(player);   // fresh state for the window
    }

    /** Gives everything in its pockets to the player. */
    private void handOver(Player player) {
        for (int i = 0; i < this.pocket.getContainerSize(); i++) {
            ItemStack stack = this.pocket.removeItemNoUpdate(i);
            if (!stack.isEmpty() && !player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    // ---------------------------------------------------------------- work

    /** Makes this child a worker of {@code player}: it follows them until told where to work. */
    public void employ(Player player, WorkJob job) {
        this.owner = player.getUUID();
        this.setJob(job);
        this.setWorkplace(null);
        this.setPersistenceRequired();
    }

    public boolean isOwner(Player player) {
        return this.owner != null && this.owner.equals(player.getUUID());
    }

    @Nullable
    public UUID getOwnerId() {
        return this.owner;
    }

    @Nullable
    private Player owner() {
        return this.owner == null ? null : this.level().getPlayerByUUID(this.owner);
    }

    /** A worker without a workplace follows its owner. */
    @Override
    @Nullable
    public Player getLeader() {
        return this.workplace == null ? this.owner() : null;
    }

    @Override
    public AbstractGoblin asGoblin() {
        return this;
    }

    @Override
    public WorkJob getJob() {
        return this.job;
    }

    public void setJob(WorkJob job) {
        this.job = job;
        this.setItemSlot(EquipmentSlot.MAINHAND, job.tool());
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    @Nullable
    public BlockPos getWorkplace() {
        return this.workplace;
    }

    /** Where to work (it sleeps there at night too), or null to follow the owner. */
    public void setWorkplace(@Nullable BlockPos pos) {
        this.workplace = pos == null ? null : pos.immutable();
        if (pos != null) {
            this.setHome(pos);
        } else {
            this.clearHome();
        }
    }

    @Override
    public int workRadius() {
        return WORK_RADIUS;
    }

    @Override
    public boolean isWorking() {
        return this.owner != null && this.workplace != null && this.job != WorkJob.NONE;
    }

    @Override
    public SimpleContainer getPocket() {
        return this.pocket;
    }

    public int itemsCarried() {
        return WorkSkills.itemsCarried(this);
    }

    public Problem problem() {
        if (this.workplace == null) {
            return Problem.NONE;
        }
        boolean chest = WorkSkills.findChest(this) != null;
        if (this.job == WorkJob.MINER && !WorkSkills.isMine(this.level(), this.workplace)) {
            return Problem.NOT_A_MINE;
        }
        if (this.job == WorkJob.FARMER && WorkSkills.findCrop(this, false) == null && WorkSkills.findCrop(this, true) == null
                && WorkSkills.findEmptyField(this) == null) {
            return Problem.NO_FIELD;
        }
        if (!chest) {
            return WorkSkills.pocketFull(this) ? Problem.FULL : Problem.NO_CHEST;
        }
        return Problem.NONE;
    }

    @Override
    public void complain(String topic) {
        long now = this.level().getGameTime();
        if (now >= this.nextComplaint) {
            this.nextComplaint = now + COMPLAINT_TICKS;
            this.sayRandom(topic);
        }
    }

    @Override
    public void onDelivered(ItemStack stack) {
        Player owner = this.owner();
        if (owner != null) {
            GoblinAdvancements.award(owner, this.job == WorkJob.MINER ? GoblinAdvancements.HAUL : GoblinAdvancements.HARVEST);
        }
    }

    @Override
    public void onFound(ItemStack stack) {
        if (stack.is(Items.DIAMOND)) {
            this.sayRandom("diamond");
            Player owner = this.owner();
            if (owner != null) {
                GoblinAdvancements.award(owner, GoblinAdvancements.DIAMOND);
            }
        }
    }

    /** A miner's haul straight into the pockets (what the work goal does after {@link WorkSkills#HAUL_TICKS} of digging). */
    public void finishHaul() {
        if (this.workplace != null) {
            for (ItemStack stack : WorkSkills.haul(this.random, this.workplace.getY())) {
                this.onFound(stack);
                WorkSkills.pocketOrDrop(this, stack);
            }
        }
    }

    public boolean deposit(BlockPos pos) {
        return WorkSkills.deposit(this, pos);
    }

    // ---------------------------------------------------------------- saving

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", this.getVariant());
        if (this.owner != null) {
            tag.putUUID("Owner", this.owner);
        }
        tag.putInt("Job", this.job.ordinal());
        if (this.workplace != null) {
            tag.put("Workplace", NbtUtils.writeBlockPos(this.workplace));
        }
        tag.put("Pocket", this.pocket.createTag(this.registryAccess()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.setVariant(tag.getInt("Variant"));
        if (tag.hasUUID("Owner")) {
            this.owner = tag.getUUID("Owner");
        }
        this.job = WorkJob.byId(tag.getInt("Job"));
        this.workplace = NbtUtils.readBlockPos(tag, "Workplace").orElse(null);
        this.pocket.fromTag(tag.getList("Pocket", Tag.TAG_COMPOUND), this.registryAccess());
        if (this.workplace != null) {
            this.setHome(this.workplace);   // again, now that the owner is known (it sets the work radius)
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        Containers.dropContents(level, this, this.pocket);
    }
}
