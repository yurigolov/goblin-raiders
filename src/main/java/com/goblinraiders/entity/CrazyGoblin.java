package com.goblinraiders.entity;

import com.goblinraiders.registry.ModSounds;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.level.Level;

/**
 * Skin 7: as small as a small goblin, faster than anything else in the village and twice as weak as a zombie
 * (1.5 damage). Hops around, cackles and babbles all the time.
 */
public class CrazyGoblin extends AbstractGoblin {
    public CrazyGoblin(EntityType<? extends CrazyGoblin> type, Level level) {
        super(type, level);
        this.xpReward = 3;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return goblinAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.42)
                .add(Attributes.ATTACK_DAMAGE, 1.5)
                .add(Attributes.FOLLOW_RANGE, 28.0);
    }

    @Override
    protected void registerCombatGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, false));
        this.addHostileTargets();
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // Erratic hops while running about.
        if (!this.level().isClientSide && this.onGround() && this.getDeltaMovement().horizontalDistanceSqr() > 0.003
                && this.random.nextInt(this.getTarget() != null ? 10 : 25) == 0) {
            this.getJumpControl().jump();
        }
    }

    /** Crazy goblins never sleep. */
    @Override
    protected int sleepersPercent() {
        return 0;
    }

    @Override
    public String voice() {
        return "crazy";
    }

    @Override
    protected int idleLineChance() {
        return 500;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 60;
    }

    @Override
    protected float pitchFactor() {
        return 1.15F;   // the recorded cackle is already high
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.random.nextInt(3) == 0 ? super.getAmbientSound() : ModSounds.CRAZY_LAUGH.get();
    }

    @Override
    protected SoundEvent getAngrySound() {
        return ModSounds.CRAZY_LAUGH.get();
    }

    @Override
    protected SoundEvent getCelebrateSound() {
        return ModSounds.CRAZY_LAUGH.get();
    }
}
