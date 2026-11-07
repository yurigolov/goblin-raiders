package com.goblinraiders.raid;

import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.Config;
import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.item.GoblinNoteItem;
import com.goblinraiders.registry.ModItems;
import com.goblinraiders.registry.ModSounds;
import com.goblinraiders.registry.ModTags;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The raid mechanic. Chests and barrels a player places (or opens, outside of generated structures) form their base.
 * While the player is far away, goblins may come, empty some of those chests, leave a note with the coordinates of their
 * village and carry the loot to that village's stash.
 */
@EventBusSubscriber(modid = GoblinRaiders.MODID)
public final class GoblinRaids {

    public enum Result {
        SUCCESS, DISABLED, NO_BASE, NOT_DUE, TOO_CLOSE, NO_VILLAGE, NOTHING_TO_STEAL, VASSAL
    }

    public record Outcome(Result result, int stacks, int chests, @Nullable BlockPos village, List<ItemStack> loot) {
        static Outcome of(Result result) {
            return new Outcome(result, 0, 0, null, List.of());
        }
    }

    /** One of eight compass directions from {@code from} to {@code to}, as a translation key suffix. */
    public static String compass(BlockPos from, BlockPos to) {
        double angle = Math.toDegrees(Math.atan2(to.getX() - from.getX(), -(to.getZ() - from.getZ())));
        String[] names = {"north", "north_east", "east", "south_east", "south", "south_west", "west", "north_west"};
        return names[(int) Math.floorMod(Math.round(angle / 45.0), 8)];
    }

    public static boolean isRaidable(BlockState state) {
        return state.getBlock() instanceof ChestBlock || state.getBlock() instanceof BarrelBlock;
    }

    // ---------------------------------------------------------------- tracking the base

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getLevel() instanceof ServerLevel level
                && level.dimension() == Level.OVERWORLD && isRaidable(event.getPlacedBlock())) {
            GoblinRaidData.get(level.getServer()).trackChest(player.getUUID(), event.getPos(), level.getGameTime());
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(event.getLevel() instanceof ServerLevel level)
                || level.dimension() != Level.OVERWORLD) {
            return;
        }
        BlockPos pos = event.getPos();
        if (!isRaidable(level.getBlockState(pos))) {
            return;
        }
        // Loot chests of dungeons, villages etc. are not the player's base.
        if (level.getBlockEntity(pos) instanceof RandomizableContainerBlockEntity container && container.getLootTable() != null) {
            return;
        }
        if (level.structureManager().getStructureWithPieceAt(pos, structure -> true).isValid()) {
            return;
        }
        GoblinRaidData.get(level.getServer()).trackChest(player.getUUID(), pos, level.getGameTime());
    }

    @SubscribeEvent
    public static void onBlockBroken(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD && isRaidable(event.getState())) {
            GoblinRaidData.get(level.getServer()).untrackChest(event.getPos());
        }
    }

    // ---------------------------------------------------------------- raiding

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (!Config.RAIDS_ENABLED.get() || server.getTickCount() % Config.CHECK_INTERVAL_TICKS.get() != 0) {
            return;
        }
        for (ServerPlayer player : List.copyOf(server.getPlayerList().getPlayers())) {
            if (!player.isSpectator()) {
                tryRaid(player, false);
            }
        }
    }

    /**
     * Raids the player's base if all conditions hold. {@code forced} skips the chance, cooldown and distance checks
     * (used by the /goblinraid command).
     */
    public static Outcome tryRaid(ServerPlayer player, boolean forced) {
        ServerLevel level = player.server.overworld();
        GoblinRaidData data = GoblinRaidData.get(player.server);
        GoblinRaidData.PlayerBase base = data.base(player.getUUID());
        if (base == null || base.chests().isEmpty()) {
            return Outcome.of(Result.NO_BASE);
        }
        long now = level.getGameTime();
        if (data.vassal(player.getUUID()) != null) {
            return Outcome.of(Result.VASSAL);   // the king's vassals are not raided while they pay tribute
        }
        if (!forced) {
            if (level.getDifficulty() == Difficulty.PEACEFUL) {
                return Outcome.of(Result.DISABLED);
            }
            if (now - base.lastRaid() < Config.COOLDOWN_TICKS.get()) {
                return Outcome.of(Result.NOT_DUE);
            }
        }

        List<BlockPos> chests = mainBase(base.chests(), Config.BASE_RADIUS.get());
        BlockPos center = centerOf(chests);
        if (!forced) {
            if (player.level() == level && horizontalDistanceSqr(player.blockPosition(), center) < square(Config.MIN_DISTANCE.get())) {
                return Outcome.of(Result.TOO_CLOSE);
            }
            if (player.getRandom().nextDouble() >= Config.RAID_CHANCE.get()) {
                return Outcome.of(Result.NOT_DUE);
            }
        }

        BlockPos village = level.findNearestMapStructure(ModTags.GOBLIN_VILLAGES, center, Config.VILLAGE_SEARCH_RADIUS.get(), false);
        if (village == null) {
            return Outcome.of(Result.NO_VILLAGE);
        }

        boolean caravan = Config.CARAVANS.get();
        Outcome outcome = robChests(level, chests, village, Config.MAX_CHESTS.get(), Config.STEAL_FRACTION.get(), !caravan);
        if (outcome.result() != Result.SUCCESS) {
            return outcome;
        }
        data.markRaided(player.getUUID(), now);
        GoblinAdvancements.award(player, GoblinAdvancements.ROBBED);
        if (caravan) {
            // The loot leaves on foot: catch the caravan before it reaches the village.
            BlockPos from = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center);
            Caravans.start(level, player.getUUID(), from, village, outcome.loot());
            Component direction = Component.translatable("goblinraiders.direction." + compass(center, village));
            player.sendSystemMessage(Component.translatable("goblinraiders.raid.caravan", direction).withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        } else {
            player.sendSystemMessage(Component.translatable("goblinraiders.raid.message").withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        }
        player.playNotifySound(ModSounds.RAID_ALARM.get(), SoundSource.HOSTILE, 1.0F, 1.0F);
        GoblinRaiders.LOGGER.info("Goblins raided {}: took {} stacks from {} chests around {} to the village at {}",
                player.getGameProfile().getName(), outcome.stacks(), outcome.chests(), center.toShortString(), village.toShortString());
        return outcome;
    }

    /**
     * Empties up to {@code maxChests} of the given chests (a {@code fraction} of the stacks of each), leaves a note in
     * each one and hands the loot to the village whose structure starts in the chunk of {@code village}.
     */
    public static Outcome robChests(ServerLevel level, List<BlockPos> chests, BlockPos village, int maxChests, double fraction) {
        return robChests(level, chests, village, maxChests, fraction, true);
    }

    /** Same, but with {@code deliver == false} the loot is only returned (a caravan will carry it). */
    public static Outcome robChests(ServerLevel level, List<BlockPos> chests, BlockPos village, int maxChests, double fraction,
                                    boolean deliver) {
        GoblinRaidData data = GoblinRaidData.get(level.getServer());
        RandomSource random = level.getRandom();
        List<BlockPos> order = new ArrayList<>(chests);
        Util.shuffle(order, random);

        List<ItemStack> loot = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        int robbed = 0;
        for (BlockPos pos : order) {
            if (robbed >= maxChests) {
                break;
            }
            if (!visited.add(pos)) {
                continue;   // the other half of a double chest robbed just now
            }
            // getBlockEntity loads the chunk if nobody is around.
            BlockState state = level.getBlockState(pos);
            if (!isRaidable(state) || !(level.getBlockEntity(pos) instanceof BaseContainerBlockEntity half)) {
                data.untrackChest(pos);
                continue;
            }
            // A double chest is one chest with one note, whichever half the goblins came to.
            Container container = half;
            if (state.getBlock() instanceof ChestBlock chest && state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
                visited.add(pos.relative(ChestBlock.getConnectedDirection(state)));
                container = Objects.requireNonNullElse(ChestBlock.getContainer(chest, state, level, pos, true), half);
            }
            List<ItemStack> taken = steal(container, random, fraction);
            if (taken.isEmpty()) {
                continue;
            }
            loot.addAll(taken);
            putNote(container, GoblinNoteItem.create(village.getX(), village.getZ()));
            container.setChanged();
            robbed++;
        }
        if (loot.isEmpty()) {
            return Outcome.of(Result.NOTHING_TO_STEAL);
        }
        if (deliver) {
            data.addStolen(new ChunkPos(village), loot);
        }
        return new Outcome(Result.SUCCESS, loot.size(), robbed, village, loot);
    }

    private static List<ItemStack> steal(Container container, RandomSource random, double fraction) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && !stack.is(ModTags.RAID_PROOF) && !stack.is(ModItems.GOBLIN_NOTE.get())) {
                slots.add(i);
            }
        }
        if (slots.isEmpty()) {
            return List.of();
        }
        Util.shuffle(slots, random);
        int count = Math.max(1, (int) Math.ceil(slots.size() * fraction));
        List<ItemStack> taken = new ArrayList<>();
        for (int slot : slots.subList(0, count)) {
            taken.add(container.removeItemNoUpdate(slot));
        }
        return taken;
    }

    /** Puts the note in the middle slot (or the first free one), unless the same note is already there. */
    private static void putNote(Container container, ItemStack note) {
        int size = container.getContainerSize();
        for (int i = 0; i < size; i++) {
            if (ItemStack.isSameItemSameComponents(container.getItem(i), note)) {
                return;
            }
        }
        int middle = (size / 9 - 1) / 2 * 9 + 4;    // 13 in a chest, 22 in a double chest
        if (middle < size && container.getItem(middle).isEmpty()) {
            container.setItem(middle, note);
            return;
        }
        for (int i = 0; i < size; i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, note);
                return;
            }
        }
    }

    /** The biggest group of chests that lie within {@code radius} of one of them. */
    static List<BlockPos> mainBase(List<BlockPos> chests, int radius) {
        long r2 = square(radius);
        List<BlockPos> best = List.of();
        for (BlockPos c : chests) {
            List<BlockPos> group = chests.stream().filter(o -> o.distSqr(c) <= r2).toList();
            if (group.size() > best.size()) {
                best = group;
            }
        }
        return best;
    }

    static BlockPos centerOf(List<BlockPos> positions) {
        long x = 0, y = 0, z = 0;
        for (BlockPos p : positions) {
            x += p.getX();
            y += p.getY();
            z += p.getZ();
        }
        int n = positions.size();
        return new BlockPos((int) (x / n), (int) (y / n), (int) (z / n));
    }

    private static long horizontalDistanceSqr(BlockPos a, BlockPos b) {
        return square(a.getX() - b.getX()) + square(a.getZ() - b.getZ());
    }

    private static long square(long v) {
        return v * v;
    }

    private GoblinRaids() {
    }
}
