package com.goblinraiders.effect;

import com.goblinraiders.GoblinRaiders;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** Stunned: no walking, no jumping, slow swings; little stars circle the head. */
public class StunnedEffect extends MobEffect {
    public StunnedEffect() {
        super(MobEffectCategory.HARMFUL, 0xE8D25A);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, GoblinRaiders.id("stunned_speed"), -1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.JUMP_STRENGTH, GoblinRaiders.id("stunned_jump"), -1.0,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.ATTACK_SPEED, GoblinRaiders.id("stunned_attack_speed"), -0.6,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 4 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            double angle = entity.tickCount * 0.6;
            double r = entity.getBbWidth() * 0.7;
            level.sendParticles(ParticleTypes.CRIT, entity.getX() + Math.cos(angle) * r, entity.getY() + entity.getBbHeight() + 0.25,
                    entity.getZ() + Math.sin(angle) * r, 1, 0.0, 0.0, 0.0, 0.0);
        }
        return true;
    }
}
