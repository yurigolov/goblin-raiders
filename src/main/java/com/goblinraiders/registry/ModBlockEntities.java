package com.goblinraiders.registry;

import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.block.BuildingSiteBlockEntity;
import com.goblinraiders.block.GoblinStashBlockEntity;
import com.goblinraiders.block.StorehouseCrateBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTER = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, GoblinRaiders.MODID);

    @SuppressWarnings("DataFlowIssue") // the data fixer type may be null
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GoblinStashBlockEntity>> GOBLIN_STASH = REGISTER.register("goblin_stash",
            () -> BlockEntityType.Builder.of(GoblinStashBlockEntity::new, ModBlocks.GOBLIN_STASH.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BuildingSiteBlockEntity>> BUILDING_SITE = REGISTER.register("building_site",
            () -> BlockEntityType.Builder.of(BuildingSiteBlockEntity::new, ModBlocks.BUILDING_SITE.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StorehouseCrateBlockEntity>> STOREHOUSE_CRATE = REGISTER.register("storehouse_crate",
            () -> BlockEntityType.Builder.of(StorehouseCrateBlockEntity::new, ModBlocks.STOREHOUSE_CRATE.get()).build(null));

    private ModBlockEntities() {
    }
}
