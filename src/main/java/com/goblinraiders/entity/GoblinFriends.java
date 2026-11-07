package com.goblinraiders.entity;

import com.goblinraiders.raid.Horde;
import com.goblinraiders.registry.ModEffects;
import com.goblinraiders.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Who goblins leave alone: players in goblin disguise (the amulet of concealment, members of the king's horde)
 * and whoever wears the goblin king's crown. Friends are only attacked when they start a fight themselves.
 */
public final class GoblinFriends {
    public static boolean isFriend(LivingEntity entity) {
        return entity instanceof Player player
                && (player.hasEffect(ModEffects.DISGUISE) || player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.GOBLIN_CROWN.get()));
    }

    /** The player just took on a goblin's shape: goblins chasing them lose track, unless they hold a grudge. */
    public static void calmGoblinsAround(Player player) {
        for (AbstractGoblin goblin : player.level().getEntitiesOfClass(AbstractGoblin.class, player.getBoundingBox().inflate(32.0),
                g -> g.getTarget() == player && !g.holdsGrudge(player))) {
            goblin.setTarget(null);
        }
    }

    /** The player hit a goblin or insulted a trader: the disguise falls off (and the horde stops renewing it for a while). */
    public static void betray(Player player) {
        if (player.level().isClientSide) {
            return;
        }
        if (player.hasEffect(ModEffects.DISGUISE)) {
            player.removeEffect(ModEffects.DISGUISE);
            player.displayClientMessage(Component.translatable("goblinraiders.disguise.broken").withStyle(ChatFormatting.RED), true);
        }
        Horde.disgrace(player);
    }

    private GoblinFriends() {
    }
}
