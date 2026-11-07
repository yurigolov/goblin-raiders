package com.goblinraiders.registry;

import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.block.BuildingSiteBlock;
import com.goblinraiders.block.GoblinStashBlock;
import com.goblinraiders.block.StorehouseCrateBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks REGISTER = DeferredRegister.createBlocks(GoblinRaiders.MODID);

    public static final DeferredBlock<GoblinStashBlock> GOBLIN_STASH = REGISTER.register("goblin_stash",
            () -> new GoblinStashBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_GREEN)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    public static final DeferredBlock<BuildingSiteBlock> BUILDING_SITE = REGISTER.register("building_site",
            () -> new BuildingSiteBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(0.5F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .ignitedByLava()));

    public static final DeferredBlock<StorehouseCrateBlock> STOREHOUSE_CRATE = REGISTER.register("storehouse_crate",
            () -> new StorehouseCrateBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .instrument(NoteBlockInstrument.BASS)
                    .strength(2.5F)
                    .sound(SoundType.WOOD)
                    .ignitedByLava()));

    private ModBlocks() {
    }
}
