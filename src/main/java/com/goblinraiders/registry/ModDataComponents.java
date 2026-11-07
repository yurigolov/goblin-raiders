package com.goblinraiders.registry;

import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.item.RaidNote;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents REGISTER =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, GoblinRaiders.MODID);

    /** Where the goblins that wrote the note took the loot. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<RaidNote>> RAID_NOTE =
            REGISTER.registerComponentType("raid_note", b -> b.persistent(RaidNote.CODEC).networkSynchronized(RaidNote.STREAM_CODEC));

    /** The place a teleportation amulet is bound to. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> AMULET_TARGET =
            REGISTER.registerComponentType("amulet_target", b -> b.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    private ModDataComponents() {
    }
}
