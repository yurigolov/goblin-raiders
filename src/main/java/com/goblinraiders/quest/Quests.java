package com.goblinraiders.quest;

import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.entity.CrazyGoblin;
import com.goblinraiders.entity.GiantGoblin;
import com.goblinraiders.entity.GoblinShaman;
import com.goblinraiders.entity.RuthlessGoblin;
import com.goblinraiders.entity.SmallGoblin;
import com.goblinraiders.registry.ModItems;
import com.goblinraiders.registry.ModTags;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Errands from the headman of a peaceful village. A player has at most one at a time: bring goods (they go into the
 * village storehouse), hunt some monsters or raiders, see the sage, scout a raiders' den or catch a raid caravan. Done
 * errands are reported to any headman for goblin coins and a bowl of grub. Kept in the player's persistent data.
 */
@EventBusSubscriber(modid = GoblinRaiders.MODID)
public final class Quests {
    public enum Kind { DELIVER, HUNT, SAGE, DEN, CARAVAN }

    /** One errand: what to do, how many times, and how many coins it pays. */
    public record Quest(String id, Kind kind, int count, int coins) {
    }

    public static final List<Quest> ALL = List.of(
            new Quest("logs", Kind.DELIVER, 24, 4),
            new Quest("cobblestone", Kind.DELIVER, 48, 3),
            new Quest("wheat", Kind.DELIVER, 16, 3),
            new Quest("iron", Kind.DELIVER, 6, 6),
            new Quest("coal", Kind.DELIVER, 16, 3),
            new Quest("zombies", Kind.HUNT, 6, 4),
            new Quest("skeletons", Kind.HUNT, 5, 5),
            new Quest("spiders", Kind.HUNT, 5, 4),
            new Quest("raiders", Kind.HUNT, 3, 6),
            new Quest("sage", Kind.SAGE, 1, 5),
            new Quest("den", Kind.DEN, 1, 4),
            new Quest("caravan", Kind.CARAVAN, 1, 8));

    /** Ten errands done make the player the headman's right hand. */
    public static final int RIGHT_HAND = 10;
    private static final String KEY = "goblinraiders.quest";
    private static final String DONE_KEY = "goblinraiders.quests_done";
    private static final String LAST_KEY = "goblinraiders.last_quest";

    // ---------------------------------------------------------------- what counts

    public static boolean delivers(Quest quest, ItemStack stack) {
        return switch (quest.id()) {
            case "logs" -> stack.is(ItemTags.LOGS);
            case "cobblestone" -> stack.is(Items.COBBLESTONE);
            case "wheat" -> stack.is(Items.WHEAT);
            case "iron" -> stack.is(Items.IRON_INGOT);
            case "coal" -> stack.is(Items.COAL);
            default -> false;
        };
    }

    public static boolean isPrey(Quest quest, Entity entity) {
        return switch (quest.id()) {
            case "zombies" -> entity.getType().is(EntityTypeTags.ZOMBIES);
            case "skeletons" -> entity.getType().is(EntityTypeTags.SKELETONS);
            case "spiders" -> entity instanceof Spider;
            case "raiders" -> entity instanceof SmallGoblin || entity instanceof RuthlessGoblin || entity instanceof GiantGoblin
                    || entity instanceof CrazyGoblin || entity instanceof GoblinShaman;
            default -> false;
        };
    }

    // ---------------------------------------------------------------- the player's errand

    private static CompoundTag data(Player player) {
        return player.getPersistentData();
    }

    @Nullable
    public static Quest current(Player player) {
        if (!data(player).contains(KEY)) {
            return null;
        }
        String id = data(player).getCompound(KEY).getString("Id");
        return byId(id).orElse(null);
    }

    public static Optional<Quest> byId(String id) {
        return ALL.stream().filter(q -> q.id().equals(id)).findFirst();
    }

    /** How far along it is: goods in the inventory for deliveries, otherwise what was done so far. */
    public static int progress(Player player) {
        Quest quest = current(player);
        if (quest == null) {
            return 0;
        }
        if (quest.kind() == Kind.DELIVER) {
            int count = 0;
            for (ItemStack stack : player.getInventory().items) {
                if (delivers(quest, stack)) {
                    count += stack.getCount();
                }
            }
            return Math.min(count, quest.count());
        }
        return data(player).getCompound(KEY).getInt("Progress");
    }

    public static boolean isDone(Player player) {
        Quest quest = current(player);
        return quest != null && progress(player) >= quest.count();
    }

    public static int completed(Player player) {
        return data(player).getInt(DONE_KEY);
    }

    /** Hands out a new errand (never the same as the last one). */
    public static Quest take(ServerPlayer player, RandomSource random) {
        Quest current = current(player);
        if (current != null) {
            return current;
        }
        String last = data(player).getString(LAST_KEY);
        List<Quest> choice = new ArrayList<>(ALL.stream().filter(q -> !q.id().equals(last)).toList());
        Quest quest = choice.get(random.nextInt(choice.size()));
        assign(player, quest);
        return quest;
    }

    /** Gives the player this very errand (the headman picks at random; tests pick). */
    public static void assign(Player player, Quest quest) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", quest.id());
        tag.putInt("Progress", 0);
        data(player).put(KEY, tag);
    }

    public static void drop(ServerPlayer player) {
        data(player).remove(KEY);
    }

    /**
     * Reports a done errand: deliveries hand over the goods (returned so the headman can put them in the storehouse),
     * and the player gets the coins and a bowl of grub. Null when there is nothing to report.
     */
    @Nullable
    public static List<ItemStack> turnIn(ServerPlayer player) {
        Quest quest = current(player);
        if (quest == null || !isDone(player)) {
            return null;
        }
        List<ItemStack> goods = new ArrayList<>();
        if (quest.kind() == Kind.DELIVER) {
            int left = quest.count();
            for (ItemStack stack : player.getInventory().items) {
                if (left > 0 && delivers(quest, stack)) {
                    goods.add(stack.split(Math.min(left, stack.getCount())));
                    left -= goods.get(goods.size() - 1).getCount();
                }
            }
        }
        give(player, new ItemStack(ModItems.GOBLIN_COIN.get(), quest.coins()));
        give(player, new ItemStack(ModItems.GOBLIN_GRUB.get(), 2));
        data(player).remove(KEY);
        data(player).putString(LAST_KEY, quest.id());
        int done = completed(player) + 1;
        data(player).putInt(DONE_KEY, done);
        GoblinAdvancements.award(player, GoblinAdvancements.QUEST);
        if (done >= RIGHT_HAND) {
            GoblinAdvancements.award(player, GoblinAdvancements.RIGHT_HAND);
        }
        return goods;
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // ---------------------------------------------------------------- progress

    /** One more step of an errand of {@code kind} (that {@code counts}); tells the player how it goes. */
    public static void advance(ServerPlayer player, Kind kind, java.util.function.Predicate<Quest> counts) {
        Quest quest = current(player);
        if (quest == null || quest.kind() != kind || !counts.test(quest) || isDone(player)) {
            return;
        }
        CompoundTag tag = data(player).getCompound(KEY);
        int progress = tag.getInt("Progress") + 1;
        tag.putInt("Progress", progress);
        data(player).put(KEY, tag);
        Component name = Component.translatable("goblinraiders.quest." + quest.id() + ".title");
        if (progress >= quest.count()) {
            player.displayClientMessage(Component.translatable("goblinraiders.quest.done", name).withStyle(ChatFormatting.GOLD), true);
        } else {
            player.displayClientMessage(Component.translatable("goblinraiders.quest.progress", name, progress, quest.count()), true);
        }
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            advance(player, Kind.HUNT, quest -> isPrey(quest, event.getEntity()));
        }
    }

    /** Scouting a den: being inside a raiders' village counts. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 40 != 0) {
            return;
        }
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            Quest quest = current(player);
            if (quest != null && quest.kind() == Kind.DEN && !isDone(player)
                    && player.serverLevel().structureManager().getStructureWithPieceAt(player.blockPosition(), ModTags.GOBLIN_VILLAGES).isValid()) {
                advance(player, Kind.DEN, q -> true);
            }
        }
    }

    /** The errand survives death (persistent data is per player entity). */
    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag from = event.getOriginal().getPersistentData();
        for (String key : new String[] {KEY, DONE_KEY, LAST_KEY}) {
            if (from.contains(key)) {
                event.getEntity().getPersistentData().put(key, from.get(key).copy());
            }
        }
    }

    public static void onSageVisit(ServerPlayer player) {
        advance(player, Kind.SAGE, q -> true);
    }

    public static void onCaravanCarrier(ServerPlayer player) {
        advance(player, Kind.CARAVAN, q -> true);
    }

    private Quests() {
    }
}
