package com.goblinraiders.item;

import com.goblinraiders.client.NoteScreens;
import com.goblinraiders.registry.ModDataComponents;
import com.goblinraiders.registry.ModItems;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Left in robbed chests. Right click opens it like a book page. */
public class GoblinNoteItem extends Item {
    public GoblinNoteItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(int x, int z) {
        ItemStack stack = new ItemStack(ModItems.GOBLIN_NOTE.get());
        stack.set(ModDataComponents.RAID_NOTE.get(), new RaidNote(x, z));
        return stack;
    }

    /** The text of the note, as one book page. */
    public static Component pageText(@Nullable RaidNote note) {
        if (note == null) {
            return Component.translatable("goblinraiders.note.blank");
        }
        return Component.translatable("goblinraiders.note.text",
                Component.literal(String.valueOf(note.x())).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
                Component.literal(String.valueOf(note.z())).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            NoteScreens.open(pageText(stack.get(ModDataComponents.RAID_NOTE.get())));
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        RaidNote note = stack.get(ModDataComponents.RAID_NOTE.get());
        if (note != null) {
            tooltip.add(Component.translatable("item.goblinraiders.goblin_note.coords", note.x(), note.z()).withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable("item.goblinraiders.goblin_note.hint").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
