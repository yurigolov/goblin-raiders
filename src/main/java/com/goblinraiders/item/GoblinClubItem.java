package com.goblinraiders.item;

import net.minecraft.world.entity.player.Player;
import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.registry.ModEffects;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Heavy and slow; every hit stuns the target for a couple of seconds (see {@link ModEffects#stun}). */
public class GoblinClubItem extends Item {
    public GoblinClubItem(Properties properties) {
        super(properties);
    }

    /** 8 damage, 0.8 attacks per second (an iron axe is 9 and 0.9). */
    public static ItemAttributeModifiers createAttributes() {
        return ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE, new AttributeModifier(BASE_ATTACK_DAMAGE_ID, 7.0, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED, new AttributeModifier(BASE_ATTACK_SPEED_ID, -3.2, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build();
    }

    /** What a club hit does on top of the damage; used for players and for goblins swinging one. */
    public static boolean onHit(LivingEntity target) {
        if (ModEffects.stun(target)) {
            target.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
                    SoundSource.PLAYERS, 0.8F, 1.3F);
            return true;
        }
        return false;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (onHit(target) && attacker instanceof Player player) {
            GoblinAdvancements.award(player, GoblinAdvancements.STUN);
        }
        return true;
    }

    @Override
    public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
    }

    @Override
    public int getEnchantmentValue() {
        return 8;
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repairCandidate) {
        return repairCandidate.is(ItemTags.LOGS);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.goblinraiders.goblin_club.desc", ModEffects.STUN_TICKS / 20.0).withStyle(ChatFormatting.GRAY));
    }
}
