package com.goblinraiders.block;

import com.goblinraiders.entity.work.Construction;
import com.goblinraiders.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Which blueprint a building site stands for and how many of its blocks are done. */
public class BuildingSiteBlockEntity extends BlockEntity {
    private String blueprint = "";
    private int progress;

    public BuildingSiteBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BUILDING_SITE.get(), pos, state);
    }

    /** The blueprint; a stake without one (placed by a player) gets a random one. */
    public String getBlueprint() {
        if (this.blueprint.isEmpty()) {
            long seed = this.worldPosition.asLong();
            this.blueprint = Construction.BLUEPRINTS.get((int) Math.floorMod(seed, (long) Construction.BLUEPRINTS.size()));
            this.setChanged();
        }
        return this.blueprint;
    }

    public void setBlueprint(String blueprint) {
        this.blueprint = blueprint;
        this.progress = 0;
        this.setChanged();
    }

    public int getProgress() {
        return this.progress;
    }

    public void advance() {
        this.progress++;
        this.setChanged();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.blueprint = tag.getString("Blueprint");
        this.progress = tag.getInt("Progress");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Blueprint", this.blueprint);
        tag.putInt("Progress", this.progress);
    }
}
