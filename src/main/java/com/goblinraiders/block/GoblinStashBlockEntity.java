package com.goblinraiders.block;

import com.goblinraiders.raid.GoblinRaidData;
import com.goblinraiders.registry.ModBlockEntities;
import com.goblinraiders.registry.ModTags;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class GoblinStashBlockEntity extends RandomizableContainerBlockEntity {
    public static final int SIZE = 54;

    private NonNullList<ItemStack> items = NonNullList.withSize(SIZE, ItemStack.EMPTY);

    public GoblinStashBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GOBLIN_STASH.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.goblinraiders.goblin_stash");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.sixRows(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SIZE;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, this.items, registries);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!this.trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, this.items, registries);
        }
    }

    /** The goblin village this stash stands in (its structure start chunk), or null outside of villages. */
    @Nullable
    public ChunkPos village(ServerLevel level) {
        StructureStart start = level.structureManager().getStructureWithPieceAt(this.worldPosition, ModTags.GOBLIN_VILLAGES);
        return start.isValid() ? start.getChunkPos() : null;
    }

    /** Moves the loot stolen by this village's goblins in here. Returns true if some of it did not fit. */
    /** How many stolen stacks wait for this stash in the village data. */
    public int stolenWaiting(ServerLevel level) {
        ChunkPos village = this.village(level);
        return village == null ? 0 : GoblinRaidData.get(level.getServer()).stolenStacks(village);
    }

    public boolean collectStolenLoot(ServerLevel level) {
        ChunkPos village = this.village(level);
        return village != null && this.collectStolenLoot(level, village);
    }

    public boolean collectStolenLoot(ServerLevel level, ChunkPos village) {
        GoblinRaidData data = GoblinRaidData.get(level.getServer());
        List<ItemStack> loot = data.takeStolen(village);
        if (loot.isEmpty()) {
            return false;
        }
        this.unpackLootTable(null);
        List<ItemStack> rest = new ArrayList<>();
        for (ItemStack stack : loot) {
            ItemStack left = this.insert(stack);
            if (!left.isEmpty()) {
                rest.add(left);
            }
        }
        if (!rest.isEmpty()) {
            data.addStolen(village, rest);
        }
        this.setChanged();
        return !rest.isEmpty();
    }

    /** When the stash is destroyed, the loot that never made it inside drops too. */
    public void dropStolenLoot(ServerLevel level) {
        ChunkPos village = this.village(level);
        if (village == null) {
            return;
        }
        BlockPos pos = this.worldPosition;
        for (ItemStack stack : GoblinRaidData.get(level.getServer()).takeStolen(village)) {
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), stack);
        }
    }

    private ItemStack insert(ItemStack stack) {
        for (int i = 0; i < SIZE && !stack.isEmpty(); i++) {
            ItemStack slot = this.items.get(i);
            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, stack)) {
                int moved = Math.min(stack.getCount(), slot.getMaxStackSize() - slot.getCount());
                if (moved > 0) {
                    slot.grow(moved);
                    stack.shrink(moved);
                }
            }
        }
        for (int i = 0; i < SIZE && !stack.isEmpty(); i++) {
            if (this.items.get(i).isEmpty()) {
                this.items.set(i, stack.copy());
                stack.setCount(0);
            }
        }
        return stack;
    }
}
