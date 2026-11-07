package com.goblinraiders;

import com.goblinraiders.registry.ModArmorMaterials;
import com.goblinraiders.registry.ModBlockEntities;
import com.goblinraiders.registry.ModBlocks;
import com.goblinraiders.registry.ModStructures;
import com.goblinraiders.registry.ModCreativeTabs;
import com.goblinraiders.registry.ModDataComponents;
import com.goblinraiders.registry.ModEffects;
import com.goblinraiders.registry.ModEntities;
import com.goblinraiders.registry.ModItems;
import com.goblinraiders.registry.ModSounds;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

@Mod(GoblinRaiders.MODID)
public class GoblinRaiders {
    public static final String MODID = "goblinraiders";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GoblinRaiders(IEventBus modBus, ModContainer container) {
        ModDataComponents.REGISTER.register(modBus);
        ModSounds.REGISTER.register(modBus);
        ModEffects.REGISTER.register(modBus);
        ModArmorMaterials.REGISTER.register(modBus);
        ModEntities.REGISTER.register(modBus);
        ModBlocks.REGISTER.register(modBus);
        ModItems.REGISTER.register(modBus);
        ModBlockEntities.REGISTER.register(modBus);
        ModCreativeTabs.REGISTER.register(modBus);
        ModStructures.REGISTER.register(modBus);

        modBus.addListener(ModEntities::registerAttributes);
        modBus.addListener(ModEntities::registerSpawnPlacements);

        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
