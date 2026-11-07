package com.goblinraiders.item;

import com.goblinraiders.entity.GoblinFriends;
import com.goblinraiders.registry.ModEffects;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Turns the player into a goblin for a minute: goblins leave them alone unless they start a fight. */
public class ConcealmentAmuletItem extends Item {
    public static final int DURATION_TICKS = 20 * 60;
    private static final int COOLDOWN_TICKS = 20 * 120;

    public ConcealmentAmuletItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.addEffect(new MobEffectInstance(ModEffects.DISGUISE, DURATION_TICKS, 0, false, false, true));
            GoblinFriends.calmGoblinsAround(player);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 1.0F, 1.2F);
            ((ServerLevel) level).sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.02);
            player.displayClientMessage(Component.translatable("goblinraiders.disguise.on").withStyle(ChatFormatting.DARK_GREEN), true);
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.goblinraiders.concealment_amulet.desc", DURATION_TICKS / 20).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.goblinraiders.concealment_amulet.lore").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
    }
}
