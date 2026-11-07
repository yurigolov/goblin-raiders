package com.goblinraiders.item;

import com.goblinraiders.raid.GoblinRaidData;
import com.goblinraiders.raid.Horde;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Given to the king's new vassals. Right click: how long until the next tribute. */
public class VassalCharterItem extends Item {
    public VassalCharterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer serverPlayer) {
            GoblinRaidData.Vassal vassal = Horde.vassal(serverPlayer);
            if (vassal == null) {
                player.displayClientMessage(Component.translatable("goblinraiders.horde.status_none").withStyle(ChatFormatting.GRAY), false);
            } else {
                long left = vassal.nextTribute - level.getGameTime();
                player.displayClientMessage(Component.translatable("goblinraiders.horde.status", Horde.TRIBUTE_COST, formatTime(left))
                        .withStyle(ChatFormatting.DARK_GREEN), false);
            }
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    /** "2 d. 5 h." of game time; overdue shows as 0. */
    public static Component formatTime(long ticks) {
        long hours = Math.max(0, ticks) / 1000;   // one Minecraft hour is 1000 ticks
        return Component.translatable("goblinraiders.time", hours / 24, hours % 24);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.goblinraiders.vassal_charter.desc", Horde.TRIBUTE_COST).withStyle(ChatFormatting.GRAY));
    }
}
