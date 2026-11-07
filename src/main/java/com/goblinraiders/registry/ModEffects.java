package com.goblinraiders.registry;

import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.effect.GoblinCurseEffect;
import com.goblinraiders.effect.GoblinDisguiseEffect;
import com.goblinraiders.effect.StunnedEffect;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(Registries.MOB_EFFECT, GoblinRaiders.MODID);

    /** Can't move, jump or swing properly for a moment (the goblin club). */
    public static final DeferredHolder<MobEffect, MobEffect> STUNNED = REGISTER.register("stunned", StunnedEffect::new);

    /** Looks like a goblin; goblins do not attack unprovoked (amulet of concealment, the king's horde). */
    public static final DeferredHolder<MobEffect, MobEffect> DISGUISE = REGISTER.register("goblin_disguise", GoblinDisguiseEffect::new);

    /** The shaman's curse. */
    public static final DeferredHolder<MobEffect, MobEffect> CURSE = REGISTER.register("goblin_curse", GoblinCurseEffect::new);

    /** How long a club stuns, and how long after a stun the target is immune to the next one (so giants can't chain it). */
    public static final int STUN_TICKS = 50;
    private static final int STUN_IMMUNITY_TICKS = 140;
    private static final String STUN_IMMUNE_UNTIL = "goblinraiders.stun_immune_until";

    /** Stuns {@code target} unless it was stunned a moment ago. Returns whether it got stunned. */
    public static boolean stun(LivingEntity target) {
        long now = target.level().getGameTime();
        if (target.level().isClientSide || target.getPersistentData().getLong(STUN_IMMUNE_UNTIL) > now) {
            return false;
        }
        target.getPersistentData().putLong(STUN_IMMUNE_UNTIL, now + STUN_IMMUNITY_TICKS);
        return target.addEffect(new MobEffectInstance(STUNNED, STUN_TICKS, 0, false, true, true));
    }

    private ModEffects() {
    }
}
