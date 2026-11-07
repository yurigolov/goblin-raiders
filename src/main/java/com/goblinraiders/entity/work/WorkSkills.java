package com.goblinraiders.entity.work;

import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.ChunkPos;
import com.goblinraiders.block.StorehouseCrateBlockEntity;
import com.goblinraiders.registry.ModItems;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What working goblins know how to do: carry things to chests, tend fields, dig at a rock face, fell trees and cook.
 * All of it is limited to the worker's area (its workplace and {@link GoblinWorker#workRadius()} around it).
 */
public final class WorkSkills {
    /** A miner brings up a haul every this many ticks of digging. */
    public static final int HAUL_TICKS = 600;
    /** Ticks of chopping per log. */
    public static final int CHOP_TICKS = 30;
    /** Ticks of cooking per pot of grub. */
    public static final int COOK_TICKS = 300;
    /** Seeds a farmer keeps for sowing instead of putting them in the chest. */
    private static final int SEEDS_KEPT = 16;
    /** The most logs taken from one tree (bigger ones are left alone). */
    private static final int MAX_TRUNK = 32;
    public static final Predicate<ItemStack> FOOD = s -> s.is(Items.POTATO) || s.is(Items.CARROT) || s.is(Items.BEETROOT)
            || s.is(Items.WHEAT) || s.is(Items.BROWN_MUSHROOM) || s.is(Items.RED_MUSHROOM);

    // ---------------------------------------------------------------- pockets and chests

    public static int itemsCarried(GoblinWorker worker) {
        int count = 0;
        SimpleContainer pocket = worker.getPocket();
        for (int i = 0; i < pocket.getContainerSize(); i++) {
            count += pocket.getItem(i).getCount();
        }
        return count;
    }

    public static int count(GoblinWorker worker, Predicate<ItemStack> what) {
        int count = 0;
        SimpleContainer pocket = worker.getPocket();
        for (int i = 0; i < pocket.getContainerSize(); i++) {
            if (what.test(pocket.getItem(i))) {
                count += pocket.getItem(i).getCount();
            }
        }
        return count;
    }

    public static boolean pocketFull(GoblinWorker worker) {
        SimpleContainer pocket = worker.getPocket();
        for (int i = 0; i < pocket.getContainerSize(); i++) {
            if (pocket.getItem(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** Into the pockets; whatever does not fit falls on the ground. */
    public static void pocketOrDrop(GoblinWorker worker, ItemStack stack) {
        ItemStack left = worker.getPocket().addItem(stack);
        if (!left.isEmpty()) {
            worker.asGoblin().spawnAtLocation(left);
        }
    }

    /** Removes {@code amount} matching items from the pockets; false (and nothing removed) if there are fewer. */
    public static boolean takeFromPocket(GoblinWorker worker, Predicate<ItemStack> what, int amount) {
        if (count(worker, what) < amount) {
            return false;
        }
        SimpleContainer pocket = worker.getPocket();
        for (int i = 0; i < pocket.getContainerSize() && amount > 0; i++) {
            ItemStack stack = pocket.getItem(i);
            if (what.test(stack)) {
                int take = Math.min(amount, stack.getCount());
                stack.shrink(take);
                amount -= take;
            }
        }
        return true;
    }

    /**
     * Where to put things: a storehouse crate with room in it (settlers), else the nearest chest or barrel within reach of
     * the workplace (never a goblin stash).
     */
    @Nullable
    public static BlockPos findChest(GoblinWorker worker) {
        return findContainer(worker, worker.workRadius(), WorkSkills::hasRoom);
    }

    @Nullable
    public static BlockPos findChest(GoblinWorker worker, Predicate<BlockPos> skip) {
        return findContainer(worker, worker.workRadius(), WorkSkills::hasRoom, skip);
    }

    private static boolean hasRoom(Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (container.getItem(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** The storehouse crates within the worker's store range of its workplace, nearest first (in loaded chunks). */
    public static List<BlockPos> storehouse(GoblinWorker worker) {
        BlockPos center = worker.getWorkplace();
        int range = worker.storeRange();
        if (center == null || range <= 0) {
            return List.of();
        }
        Level level = worker.asGoblin().level();
        ChunkPos chunk = new ChunkPos(center);
        int r = (range >> 4) + 1;
        List<BlockPos> crates = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (!level.hasChunk(chunk.x + dx, chunk.z + dz)) {
                    continue;
                }
                for (BlockEntity entity : level.getChunk(chunk.x + dx, chunk.z + dz).getBlockEntities().values()) {
                    if (entity instanceof StorehouseCrateBlockEntity && entity.getBlockPos().distSqr(center) <= (double) range * range) {
                        crates.add(entity.getBlockPos());
                    }
                }
            }
        }
        crates.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return crates;
    }

    /**
     * The nearest container that {@code accept}s: a storehouse crate when the worker has a storehouse, otherwise any
     * container within {@code radius} of the workplace.
     */
    @Nullable
    public static BlockPos findContainer(GoblinWorker worker, int radius, Predicate<Container> accept) {
        return findContainer(worker, radius, accept, pos -> false);
    }

    /** The same, passing over the containers the worker could not get to lately. */
    @Nullable
    public static BlockPos findContainer(GoblinWorker worker, int radius, Predicate<Container> accept, Predicate<BlockPos> skip) {
        BlockPos center = worker.getWorkplace();
        if (center == null) {
            return null;
        }
        for (BlockPos crate : storehouse(worker)) {
            Container container = containerAt(worker.asGoblin().level(), crate);
            if (container != null && !skip.test(crate) && accept.test(container)) {
                return crate;
            }
        }
        Level level = worker.asGoblin().level();
        BlockPos best = null;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -3, -radius), center.offset(radius, 3, radius))) {
            if (isStorage(level.getBlockEntity(pos))
                    && (best == null || pos.distSqr(center) < best.distSqr(center)) && !skip.test(pos)) {
                Container container = containerAt(level, pos);
                if (container != null && accept.test(container)) {
                    best = pos.immutable();
                }
            }
        }
        return best;
    }

    /** Somewhere to keep things: chests, barrels, shulker boxes, crates; not furnaces, hoppers or the raiders' stash. */
    private static boolean isStorage(@Nullable BlockEntity entity) {
        return entity instanceof ChestBlockEntity || entity instanceof BarrelBlockEntity || entity instanceof ShulkerBoxBlockEntity
                || entity instanceof StorehouseCrateBlockEntity;
    }

    public static Predicate<Container> holding(Predicate<ItemStack> what) {
        return container -> {
            for (int i = 0; i < container.getContainerSize(); i++) {
                if (what.test(container.getItem(i))) {
                    return true;
                }
            }
            return false;
        };
    }

    /** The container at {@code pos}; both halves of a double chest. */
    @Nullable
    public static Container containerAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof ChestBlock chest) {
            return ChestBlock.getContainer(chest, state, level, pos, true);
        }
        return level.getBlockEntity(pos) instanceof Container container ? container : null;
    }

    /** Whether a trip to the chest is worth it: anything but the seeds a farmer keeps for sowing. */
    public static boolean hasSomethingToDeposit(GoblinWorker worker) {
        int seeds = 0;
        SimpleContainer pocket = worker.getPocket();
        for (int i = 0; i < pocket.getContainerSize(); i++) {
            ItemStack stack = pocket.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!keeps(worker, stack)) {
                return true;
            }
            seeds += stack.getCount();
        }
        return seeds > SEEDS_KEPT;
    }

    /** What a worker keeps for its own work instead of putting it away: seeds (farmers), ingredients (cooks), materials (builders). */
    private static boolean keeps(GoblinWorker worker, ItemStack stack) {
        return switch (worker.getJob()) {
            case FARMER -> isSeed(stack);
            case COOK -> FOOD.test(stack);
            case BUILDER -> Construction.isMaterial(stack);
            default -> false;
        };
    }

    /** Puts what it carries into the container at {@code pos} (except what it keeps for its work). */
    public static boolean deposit(GoblinWorker worker, BlockPos pos) {
        Container container = containerAt(worker.asGoblin().level(), pos);
        if (container == null) {
            return false;
        }
        boolean moved = false;
        int kept = 0;
        SimpleContainer pocket = worker.getPocket();
        for (int i = 0; i < pocket.getContainerSize(); i++) {
            ItemStack stack = pocket.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (keeps(worker, stack) && kept < SEEDS_KEPT) {
                kept += stack.getCount();
                continue;
            }
            ItemStack moving = stack.copy();
            int before = stack.getCount();
            ItemStack left = HopperBlockEntity.addItem(null, container, stack, Direction.UP);
            pocket.setItem(i, left);
            if (left.getCount() < before) {
                moved = true;
                moving.setCount(before - left.getCount());
                worker.onDelivered(moving);
            }
        }
        if (moved) {
            worker.asGoblin().swing(InteractionHand.MAIN_HAND);
            Level level = worker.asGoblin().level();
            SoundEvent lid = level.getBlockEntity(pos) instanceof ChestBlockEntity ? SoundEvents.CHEST_CLOSE : SoundEvents.BARREL_CLOSE;
            level.playSound(null, pos, lid, SoundSource.BLOCKS, 0.5F, 0.9F + level.random.nextFloat() * 0.2F);
        }
        return moved;
    }

    /** Takes up to {@code max} matching items from the container at {@code pos} into the pockets. */
    public static int fetch(GoblinWorker worker, BlockPos pos, Predicate<ItemStack> what, int max) {
        Container container = containerAt(worker.asGoblin().level(), pos);
        if (container == null) {
            return 0;
        }
        int taken = 0;
        for (int i = 0; i < container.getContainerSize() && taken < max; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && what.test(stack)) {
                ItemStack part = container.removeItem(i, Math.min(max - taken, stack.getCount()));
                taken += part.getCount();
                pocketOrDrop(worker, part);
            }
        }
        if (taken > 0) {
            container.setChanged();
            worker.asGoblin().swing(InteractionHand.MAIN_HAND);
        }
        return taken;
    }

    // ---------------------------------------------------------------- the farmer

    public static boolean isSeed(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock() instanceof CropBlock;
    }

    /** The nearest ripe crop (or, with {@code anyAge}, any crop) in the fields around the workplace. */
    @Nullable
    public static BlockPos findCrop(GoblinWorker worker, boolean anyAge) {
        return nearest(worker, 2, 2, (level, pos, state) -> state.getBlock() instanceof CropBlock crop && (anyAge || crop.isMaxAge(state)));
    }

    /** The nearest farmland with nothing growing on it. */
    @Nullable
    public static BlockPos findEmptyField(GoblinWorker worker) {
        return nearest(worker, 3, 2, (level, pos, state) -> state.getBlock() instanceof FarmBlock && level.isEmptyBlock(pos.above()));
    }

    public static boolean hasSeeds(GoblinWorker worker) {
        return count(worker, WorkSkills::isSeed) > 0;
    }

    /** Picks a ripe crop, keeps the harvest and sows the same crop again. */
    public static boolean harvest(GoblinWorker worker, BlockPos pos) {
        Mob goblin = worker.asGoblin();
        BlockState state = goblin.level().getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state) || !(goblin.level() instanceof ServerLevel level)) {
            return false;
        }
        List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, null, goblin, goblin.getMainHandItem()));
        level.destroyBlock(pos, false, goblin);
        Item seed = crop.asItem();
        boolean replanted = false;
        for (ItemStack drop : drops) {
            if (!replanted && drop.is(seed)) {
                drop.shrink(1);
                replanted = true;
            }
        }
        if (!replanted) {
            replanted = takeFromPocket(worker, s -> s.is(seed), 1);
        }
        if (replanted && crop.getStateForAge(0).canSurvive(level, pos)) {
            level.setBlockAndUpdate(pos, crop.getStateForAge(0));
        }
        drops.stream().filter(d -> !d.isEmpty()).forEach(d -> pocketOrDrop(worker, d));
        goblin.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    /** Sows a seed from its pockets on empty farmland. */
    public static boolean plant(GoblinWorker worker, BlockPos farmland) {
        Mob goblin = worker.asGoblin();
        BlockPos pos = farmland.above();
        if (!goblin.level().isEmptyBlock(pos)) {
            return false;
        }
        SimpleContainer pocket = worker.getPocket();
        for (int i = 0; i < pocket.getContainerSize(); i++) {
            ItemStack stack = pocket.getItem(i);
            if (isSeed(stack) && stack.getItem() instanceof BlockItem item) {
                BlockState crop = item.getBlock().defaultBlockState();
                if (crop.canSurvive(goblin.level(), pos)) {
                    goblin.level().setBlockAndUpdate(pos, crop);
                    goblin.level().playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                    stack.shrink(1);
                    goblin.swing(InteractionHand.MAIN_HAND);
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- the miner

    /**
     * A workplace counts as a mine when it is walled in by natural stone at foot and head height - a tunnel, a cave or
     * a pit. A stone floor alone (a mountain top, a stone field) does not make a mine.
     */
    public static boolean isMine(Level level, BlockPos at) {
        int stone = 0;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-2, 0, -2), at.offset(2, 1, 2))) {
            if (level.getBlockState(pos).is(BlockTags.BASE_STONE_OVERWORLD)) {
                stone++;
            }
        }
        return stone >= 8;
    }

    /** The rock the miner chips at: natural stone next to the workplace, at its feet or head. */
    @Nullable
    public static BlockPos findRockFace(GoblinWorker worker) {
        BlockPos at = worker.getWorkplace();
        if (at == null) {
            return null;
        }
        Level level = worker.asGoblin().level();
        BlockPos best = null;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-2, 0, -2), at.offset(2, 1, 2))) {
            if (level.getBlockState(pos).is(BlockTags.BASE_STONE_OVERWORLD) && (best == null || pos.distSqr(at) < best.distSqr(at))) {
                best = pos.immutable();
            }
        }
        return best;
    }

    /** One haul from the mine: some rubble and maybe ore - the deeper the workplace, the better. */
    public static List<ItemStack> haul(RandomSource random, int y) {
        List<ItemStack> haul = new ArrayList<>();
        haul.add(new ItemStack(y < 0 ? Items.COBBLED_DEEPSLATE : Items.COBBLESTONE, 2 + random.nextInt(3)));
        float roll = random.nextFloat();
        if ((roll -= 0.015F) < 0 && y < 16) {
            haul.add(new ItemStack(Items.DIAMOND));
        } else if ((roll -= 0.02F) < 0) {
            haul.add(new ItemStack(ModItems.GOBLIN_COIN.get()));
        } else if ((roll -= 0.06F) < 0 && y < 16) {
            haul.add(new ItemStack(Items.REDSTONE, 2 + random.nextInt(3)));
        } else if ((roll -= 0.04F) < 0 && y < 32) {
            haul.add(new ItemStack(Items.LAPIS_LAZULI, 2 + random.nextInt(3)));
        } else if ((roll -= 0.06F) < 0 && y < 32) {
            haul.add(new ItemStack(Items.RAW_GOLD));
        } else if ((roll -= 0.15F) < 0) {
            haul.add(new ItemStack(Items.RAW_IRON));
        } else if ((roll -= 0.15F) < 0 && y > -16) {
            haul.add(new ItemStack(Items.RAW_COPPER, 1 + random.nextInt(2)));
        } else if ((roll -= 0.25F) < 0 && y > 0) {
            haul.add(new ItemStack(Items.COAL, 1 + random.nextInt(2)));
        }
        return haul;
    }

    // ---------------------------------------------------------------- the lumberjack

    /** The base log of the nearest real tree (a trunk on the ground with natural leaves on top) around the workplace. */
    @Nullable
    public static BlockPos findTree(GoblinWorker worker) {
        return nearest(worker, 3, 6, (level, pos, state) -> isTreeBase(level, pos));
    }

    public static boolean isTreeBase(Level level, BlockPos pos) {
        if (!level.getBlockState(pos).is(BlockTags.LOGS) || !level.getBlockState(pos.below()).is(BlockTags.DIRT)) {
            return false;
        }
        List<BlockPos> trunk = trunk(level, pos);
        return !trunk.isEmpty() && trunk.size() <= MAX_TRUNK && hasNaturalLeaves(level, trunk.get(0));
    }

    /** The logs of the tree growing from {@code base}, highest first (empty when it is too big to fell). */
    public static List<BlockPos> trunk(Level level, BlockPos base) {
        List<BlockPos> logs = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<BlockPos> open = new ArrayDeque<>();
        open.add(base.immutable());
        seen.add(base.immutable());
        while (!open.isEmpty()) {
            BlockPos pos = open.poll();
            logs.add(pos);
            if (logs.size() > MAX_TRUNK) {
                return List.of();
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (Math.abs(next.getX() - base.getX()) <= 5 && Math.abs(next.getZ() - base.getZ()) <= 5
                                && seen.add(next) && level.getBlockState(next).is(BlockTags.LOGS)) {
                            open.add(next);
                        }
                    }
                }
            }
        }
        logs.sort(Comparator.comparingInt((BlockPos p) -> p.getY()).reversed());
        return logs;
    }

    /** Leaves that grew there (not placed by a player) next to the top of the trunk. */
    private static boolean hasNaturalLeaves(Level level, BlockPos top) {
        for (BlockPos pos : BlockPos.betweenClosed(top.offset(-2, -1, -2), top.offset(2, 2, 2))) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof LeavesBlock && !state.getValue(LeavesBlock.PERSISTENT)) {
                return true;
            }
        }
        return false;
    }

    /** Chops one log into the pockets. */
    public static boolean chop(GoblinWorker worker, BlockPos pos) {
        Mob goblin = worker.asGoblin();
        BlockState state = goblin.level().getBlockState(pos);
        if (!state.is(BlockTags.LOGS) || !(goblin.level() instanceof ServerLevel level)) {
            return false;
        }
        Block.getDrops(state, level, pos, null, goblin, goblin.getMainHandItem()).forEach(d -> pocketOrDrop(worker, d));
        level.destroyBlock(pos, false, goblin);
        goblin.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    /** Plants a sapling of the felled kind where the tree stood. */
    public static boolean replant(Level level, BlockPos base, Block log) {
        if (!level.isEmptyBlock(base) || !level.getBlockState(base.below()).is(BlockTags.DIRT)) {
            return false;
        }
        BlockState sapling = saplingFor(log).defaultBlockState();
        if (!sapling.canSurvive(level, base)) {
            return false;
        }
        level.setBlockAndUpdate(base, sapling);
        return true;
    }

    /** oak_log -> oak_sapling, mangrove -> its propagule; oak when there is no such sapling. */
    public static Block saplingFor(Block log) {
        String wood = BuiltInRegistries.BLOCK.getKey(log).getPath().replace("stripped_", "").replace("_log", "").replace("_wood", "");
        String sapling = wood.equals("mangrove") ? "mangrove_propagule" : wood + "_sapling";
        return BuiltInRegistries.BLOCK.getOptional(ResourceLocation.withDefaultNamespace(sapling)).orElse(Blocks.OAK_SAPLING);
    }

    // ---------------------------------------------------------------- the cook

    /** A fire to cook on near the workplace: a campfire, a smoker or a furnace. */
    @Nullable
    public static BlockPos findKitchenFire(GoblinWorker worker) {
        BlockPos at = worker.getWorkplace();
        if (at == null) {
            return null;
        }
        Level level = worker.asGoblin().level();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-4, -1, -4), at.offset(4, 2, 4))) {
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.CAMPFIRES) || state.is(Blocks.SMOKER) || state.is(Blocks.FURNACE)) {
                return pos.immutable();
            }
        }
        return null;
    }

    /** Two ingredients from the pockets make a bowl of goblin grub. */
    public static boolean cook(GoblinWorker worker) {
        if (!takeFromPocket(worker, FOOD, 2)) {
            return false;
        }
        pocketOrDrop(worker, new ItemStack(ModItems.GOBLIN_GRUB.get()));
        return true;
    }

    // ---------------------------------------------------------------- searching

    private interface BlockTest {
        boolean test(Level level, BlockPos pos, BlockState state);
    }

    /** The matching block nearest to the worker within its area ({@code below}/{@code above} blocks around the workplace height). */
    @Nullable
    private static BlockPos nearest(GoblinWorker worker, int below, int above, BlockTest test) {
        BlockPos center = worker.getWorkplace();
        if (center == null) {
            return null;
        }
        Mob goblin = worker.asGoblin();
        Level level = goblin.level();
        int r = worker.workRadius();
        BlockPos here = goblin.blockPosition();
        BlockPos best = null;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -below, -r), center.offset(r, above, r))) {
            if ((best == null || pos.distSqr(here) < best.distSqr(here)) && test.test(level, pos, level.getBlockState(pos))) {
                best = pos.immutable();
            }
        }
        return best;
    }

    private WorkSkills() {
    }
}
