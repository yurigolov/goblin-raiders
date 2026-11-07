package com.goblinraiders.registry;

import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.world.HamletStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> REGISTER = DeferredRegister.create(Registries.STRUCTURE_TYPE, GoblinRaiders.MODID);

    /** The peaceful village: a jigsaw village that puts out the lava it stands on. */
    public static final DeferredHolder<StructureType<?>, StructureType<HamletStructure>> HAMLET = REGISTER.register("hamlet",
            () -> () -> HamletStructure.CODEC);

    private ModStructures() {
    }
}
