package com.goblinraiders.entity;

import javax.annotation.Nullable;
import com.goblinraiders.registry.ModItems;
import com.goblinraiders.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Skin 4: a 3.6 block tall brute. Slow, tough, and its punches toss the target up; 35% carry a stunning club. */
public class GiantGoblin extends AbstractGoblin {
    private static final float CLUB_CHANCE = 0.35F;

    public GiantGoblin(EntityType<? extends GiantGoblin> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return goblinAttributes()
                .add(Attributes.MAX_HEALTH, 80.0)
                .add(Attributes.MOVEMENT_SPEED, 0.19)
                .add(Attributes.ATTACK_DAMAGE, 12.0)
                .add(Attributes.ATTACK_KNOCKBACK, 1.5)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
                .add(Attributes.ARMOR, 4.0)
                .add(Attributes.STEP_HEIGHT, 1.0)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerCombatGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        this.addHostileTargets();
    }

    /** Some giants carry a club that stuns. */
    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        if (random.nextFloat() < CLUB_CHANCE) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GOBLIN_CLUB.get()));
            this.setDropChance(EquipmentSlot.MAINHAND, 0.05F);
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (hit) {
            target.setDeltaMovement(target.getDeltaMovement().add(0.0, 0.35, 0.0));
            this.playSound(ModSounds.GIANT_ATTACK.get(), 1.0F, this.getVoicePitch());
        }
        return hit;
    }

    /** Giants are lazy: most of them sleep at night. */
    @Override
    protected int sleepersPercent() {
        return 80;
    }

    @Override
    public String voice() {
        return "giant";
    }

    @Override
    protected float pitchFactor() {
        return 0.8F;
    }

    @Override
    protected float getSoundVolume() {
        return 1.5F;
    }

    @Override
    protected SoundEvent getTalkSound() {
        return ModSounds.GIANT_AMBIENT.get();
    }

    @Override
    protected SoundEvent getAngrySound() {
        return ModSounds.GIANT_ROAR.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.GIANT_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.GIANT_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.GIANT_DEATH.get();
    }

    @Override
    @Nullable
    protected SoundEvent getAttackSound() {
        return null;   // the ravager-like thump plays already
    }

    @Override
    protected SoundEvent getCelebrateSound() {
        return ModSounds.GIANT_ROAR.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(SoundEvents.RAVAGER_STEP, 0.4F, 0.8F);
    }
}
