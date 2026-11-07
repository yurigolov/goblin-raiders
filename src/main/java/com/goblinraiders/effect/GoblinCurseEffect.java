package com.goblinraiders.effect;

import com.goblinraiders.GoblinRaiders;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/** The goblin shaman's curse: weaker hits, heavier legs and bad luck for a while. */
public class GoblinCurseEffect extends MobEffect {
    public GoblinCurseEffect() {
        super(MobEffectCategory.HARMFUL, 0x6A2C7A);
        this.addAttributeModifier(Attributes.ATTACK_DAMAGE, GoblinRaiders.id("curse_damage"), -0.3,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, GoblinRaiders.id("curse_speed"), -0.15,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        this.addAttributeModifier(Attributes.LUCK, GoblinRaiders.id("curse_luck"), -2.0, AttributeModifier.Operation.ADD_VALUE);
    }
}
