package com.goblinraiders.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import com.goblinraiders.entity.work.WorkJob;
import com.goblinraiders.network.ModNetwork;
import com.goblinraiders.network.DialogChoicePayload;
import com.goblinraiders.network.OpenKingDialogPayload;
import com.goblinraiders.raid.GoblinRaidData;
import com.goblinraiders.raid.Horde;
import com.goblinraiders.registry.ModEntities;
import com.goblinraiders.registry.ModItems;
import com.goblinraiders.registry.ModSounds;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Skin 8: the goblin king, a boss three blocks tall who sits in the throne hall of the goblin city. He cannot be hurt
 * until someone talks to him: the dialog offers to join his horde (see {@link Horde}) or to die. Fighting him shows a
 * boss bar; he slams the ground, calls his guard at two and one thirds of his health, and forgets the fight if the
 * challenger runs away. Drops his crown and scepter.
 */
public class GoblinKing extends AbstractGoblin {
    public static final String CHALLENGE = "challenge";
    public static final String SUBMIT = "submit";
    public static final String TRIBUTE = "tribute";
    public static final String TRAITOR = "traitor";
    private static final int SLAM_COOLDOWN = 140;
    private static final int CALM_TICKS = 600;

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.GREEN,
            BossEvent.BossBarOverlay.NOTCHED_10).setDarkenScreen(false);
    private boolean challenged;
    private int calmTicks;
    private int slamCooldown = SLAM_COOLDOWN;
    private int guardWaves;

    public GoblinKing(EntityType<? extends GoblinKing> type, Level level) {
        super(type, level);
        this.xpReward = 150;
        this.bossEvent.setVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return goblinAttributes()
                .add(Attributes.MAX_HEALTH, 300.0)
                .add(Attributes.ATTACK_DAMAGE, 16.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerCombatGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true));
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.KING_SCEPTER.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);   // the loot table drops crown and scepter
    }

    public boolean isChallenged() {
        return this.challenged;
    }

    // ---------------------------------------------------------------- talking

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || player.getItemInHand(hand).is(Items.NAME_TAG) || !this.isAlive()) {
            return super.mobInteract(player, hand);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (this.challenged) {
                this.sayRandom(Speech.TAUNT);
            } else if (Horde.isTraitor(player)) {
                this.shoutRandom(TRAITOR, this.playersAround(32.0));
                this.challenge(player);   // no talking to traitors
            } else {
                this.startTalking(player, 20 * 60);
                openDialog(serverPlayer);
                this.playSound(ModSounds.KING_AMBIENT.get(), 1.0F, this.getVoicePitch());
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    private void openDialog(ServerPlayer player) {
        GoblinRaidData.Vassal vassal = Horde.vassal(player);
        long now = this.level().getGameTime();
        ModNetwork.send(player, new OpenKingDialogPayload(this.getId(), vassal != null,
                vassal == null ? 0 : vassal.nextTribute - now, Horde.coinsOf(player)));
    }

    /** The player's answer from the king's dialog. */
    @Override
    public void onDialogChoice(ServerPlayer player, int choice) {
        if (this.challenged) {
            return;
        }
        switch (choice) {
            case DialogChoicePayload.KING_SUBMIT -> {
                if (!Horde.isVassal(player)) {
                    Horde.submit(player);
                    this.shoutRandom(SUBMIT, List.of(player));
                }
                this.stopTalking();
            }
            case DialogChoicePayload.KING_FIGHT -> {
                if (!Horde.isVassal(player)) {
                    this.challenge(player);
                }
            }
            case DialogChoicePayload.KING_BETRAY -> {
                if (Horde.isVassal(player)) {
                    Horde.betray(player);
                    this.challenge(player);
                    this.shoutRandom(TRAITOR, this.playersAround(32.0));
                }
            }
            case DialogChoicePayload.KING_PAY -> {
                if (Horde.payTribute(player)) {
                    this.sayRandom(TRIBUTE);
                }
                this.openDialog(player);
            }
            case DialogChoicePayload.KING_HIRE_SMALL -> this.hire(player, Horde.Hireling.SMALL);
            case DialogChoicePayload.KING_HIRE_RUTHLESS -> this.hire(player, Horde.Hireling.RUTHLESS);
            case DialogChoicePayload.KING_HIRE_GIANT -> this.hire(player, Horde.Hireling.GIANT);
            case DialogChoicePayload.KING_BUY_FARMER -> {
                Horde.buyWorker(player, WorkJob.FARMER);
                this.openDialog(player);
            }
            case DialogChoicePayload.KING_BUY_MINER -> {
                Horde.buyWorker(player, WorkJob.MINER);
                this.openDialog(player);
            }
            default -> {
                if (this.getTalkingTo() == player) {
                    this.stopTalking();
                }
            }
        }
    }

    private void hire(ServerPlayer player, Horde.Hireling kind) {
        Horde.hire(player, kind);
        this.openDialog(player);
    }

    /** "Then die!" - the fight starts. */
    public void challenge(Player player) {
        this.challenged = true;
        this.calmTicks = 0;
        this.stopTalking();
        GoblinFriends.betray(player);
        this.bossEvent.setVisible(true);
        this.shoutRandom(CHALLENGE, this.playersAround(32.0));
        this.playSound(ModSounds.KING_ROAR.get(), 3.0F, 1.0F);
        this.setTarget(player);
        this.callForHelp(player, 24.0);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.challenged && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            if (!this.level().isClientSide && source.getEntity() instanceof Player player) {
                player.displayClientMessage(Component.translatable("goblinraiders.king.not_now").withStyle(ChatFormatting.GOLD), true);
            }
            return false;
        }
        return super.hurt(source, amount);
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (target != null && !this.challenged) {
            return;   // the king does not fight before he is challenged
        }
        super.setTarget(target);
    }

    // ---------------------------------------------------------------- the fight

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        if (!this.challenged) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || this.distanceToSqr(target) > 48.0 * 48.0) {
            if (++this.calmTicks > CALM_TICKS) {
                this.calmDown();
                return;
            }
        } else {
            this.calmTicks = 0;
            if (--this.slamCooldown <= 0 && this.distanceToSqr(target) < 5.0 * 5.0) {
                this.slam();
                this.slamCooldown = SLAM_COOLDOWN;
            }
        }
        float health = this.getHealth() / this.getMaxHealth();
        if (this.guardWaves == 0 && health < 0.66F || this.guardWaves == 1 && health < 0.33F) {
            this.callGuards(2 + this.guardWaves);
            this.guardWaves++;
        }
        // The boss bar is for everyone around the fight.
        for (ServerPlayer player : ((ServerLevel) this.level()).players()) {
            if (this.distanceToSqr(player) < 48.0 * 48.0) {
                this.bossEvent.addPlayer(player);
            } else {
                this.bossEvent.removePlayer(player);
            }
        }
    }

    /** Smashes the ground: damage and a launch for everyone close who is not a goblin. */
    private void slam() {
        ServerLevel level = (ServerLevel) this.level();
        this.swing(InteractionHand.MAIN_HAND);
        level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.2, this.getZ(), 8, 2.0, 0.2, 2.0, 0.0);
        this.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.5F, 0.6F);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(5.0, 2.0, 5.0),
                e -> e != this && !this.isAlliedTo(e) && e.isAlive())) {
            victim.hurt(this.damageSources().mobAttack(this), 10.0F);
            Vec3 push = victim.position().subtract(this.position()).normalize().scale(1.2);
            victim.push(push.x, 0.7, push.z);
            victim.hurtMarked = true;
        }
    }

    private void callGuards(int count) {
        ServerLevel level = (ServerLevel) this.level();
        this.sayRandom(Speech.BATTLE);
        for (int i = 0; i < count; i++) {
            RuthlessGoblin guard = ModEntities.RUTHLESS_GOBLIN.get().create(level);
            if (guard == null) {
                continue;
            }
            double a = Math.PI * 2 * i / count;
            guard.moveTo(this.getX() + Math.cos(a) * 3, this.getY(), this.getZ() + Math.sin(a) * 3, 0.0F, 0.0F);
            guard.finalizeSpawn(level, level.getCurrentDifficultyAt(guard.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            if (this.getHome() != null) {
                guard.setHome(this.getHome());
            }
            level.addFreshEntity(guard);
            guard.setTarget(this.getTarget());
            level.sendParticles(ParticleTypes.POOF, guard.getX(), guard.getY() + 1, guard.getZ(), 10, 0.3, 0.5, 0.3, 0.02);
        }
    }

    /** The challenger fled: back to the throne, healed. */
    private void calmDown() {
        this.challenged = false;
        this.calmTicks = 0;
        this.guardWaves = 0;
        this.setTarget(null);
        this.setHealth(this.getMaxHealth());
        this.bossEvent.removeAllPlayers();
        this.bossEvent.setVisible(false);
        this.sayRandom(Speech.CALM);
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        if (!this.level().isClientSide) {
            for (Player player : this.playersAround(64.0)) {
                player.sendSystemMessage(Component.translatable("goblinraiders.king.fallen").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            }
            this.bossEvent.removeAllPlayers();
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    // ---------------------------------------------------------------- saving, misc

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Challenged", this.challenged);
        tag.putInt("GuardWaves", this.guardWaves);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.challenged = tag.getBoolean("Challenged");
        this.guardWaves = tag.getInt("GuardWaves");
        this.bossEvent.setVisible(this.challenged);
    }

    @Override
    public boolean canChat() {
        return false;   // kings do not gossip
    }

    /** The king neither sleeps nor stands watch: he stays on his throne. */
    @Override
    protected int sleepersPercent() {
        return 0;
    }

    @Override
    protected boolean keepsWatch() {
        return false;
    }

    @Override
    public boolean holdsGrudge(Player player) {
        return this.challenged || super.holdsGrudge(player);
    }

    @Override
    protected int homeRadius() {
        return 8;
    }

    @Override
    public boolean isPreventingPlayerRest(Player player) {
        return this.challenged;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public String voice() {
        return "king";
    }

    @Override
    protected float pitchFactor() {
        return 0.75F;
    }

    @Override
    protected float getSoundVolume() {
        return 1.6F;
    }

    @Override
    protected SoundEvent getTalkSound() {
        return ModSounds.KING_AMBIENT.get();
    }

    @Override
    protected SoundEvent getAngrySound() {
        return ModSounds.KING_ROAR.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.KING_AMBIENT.get();
    }

    @Override
    @Nullable
    protected SoundEvent getAttackSound() {
        return null;   // the king roars and slams on his own
    }

    @Override
    protected SoundEvent getCelebrateSound() {
        return ModSounds.KING_ROAR.get();
    }

    /** Heavy royal steps. */
    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(SoundEvents.RAVAGER_STEP, 0.5F, 1.0F);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.GIANT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.KING_ROAR.get();
    }
}
