package com.goblinraiders.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The player looks like a goblin and goblins treat them as one of their own: no unprovoked attacks.
 * Hitting a goblin or refusing a trader breaks it (see {@code GoblinFriends#betray}). The look is drawn client side
 * ({@code client.DisguiseRendering}), the "friend" rule lives in {@code GoblinFriends}.
 */
public class GoblinDisguiseEffect extends MobEffect {
    public GoblinDisguiseEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x5E8C4A);
    }
}
