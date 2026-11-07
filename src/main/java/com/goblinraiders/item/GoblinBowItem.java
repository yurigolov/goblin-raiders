package com.goblinraiders.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A bow whose arrows hit half again as hard as a regular bow's. */
public class GoblinBowItem extends BowItem {
    public static final double DAMAGE_MULTIPLIER = 1.5;

    public GoblinBowItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow customArrow(AbstractArrow arrow, ItemStack projectileStack, ItemStack weaponStack) {
        arrow.setBaseDamage(arrow.getBaseDamage() * DAMAGE_MULTIPLIER);
        return arrow;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.goblinraiders.goblin_bow.desc").withStyle(ChatFormatting.GRAY));
    }
}
