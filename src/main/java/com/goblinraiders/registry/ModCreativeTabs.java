package com.goblinraiders.registry;

import com.goblinraiders.GoblinRaiders;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GoblinRaiders.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = REGISTER.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.goblinraiders"))
            .icon(() -> ModItems.GOBLIN_COIN.get().getDefaultInstance())
            .displayItems((params, output) -> {
                output.accept(ModItems.GOBLIN_SWORD.get());
                output.accept(ModItems.GOBLIN_DAGGER.get());
                output.accept(ModItems.GOBLIN_CLUB.get());
                output.accept(ModItems.GOBLIN_BOW.get());
                output.accept(ModItems.GOBLIN_HELMET.get());
                output.accept(ModItems.GOBLIN_CHESTPLATE.get());
                output.accept(ModItems.GOBLIN_LEGGINGS.get());
                output.accept(ModItems.GOBLIN_BOOTS.get());
                output.accept(ModItems.GOBLIN_CROWN.get());
                output.accept(ModItems.KING_SCEPTER.get());
                output.accept(ModItems.TELEPORT_AMULET.get());
                output.accept(ModItems.CONCEALMENT_AMULET.get());
                output.accept(ModItems.GOBLIN_GRUB.get());
                output.accept(ModItems.VASSAL_CHARTER.get());
                output.accept(ModItems.LOOT_SACK.get());
                output.accept(ModItems.GOBLIN_COIN.get());
                output.accept(ModItems.GOBLIN_NOTE.get());
                output.accept(ModItems.GOBLIN_STASH.get());
                output.accept(ModItems.SMALL_GOBLIN_SPAWN_EGG.get());
                output.accept(ModItems.RUTHLESS_GOBLIN_SPAWN_EGG.get());
                output.accept(ModItems.NEUTRAL_GOBLIN_SPAWN_EGG.get());
                output.accept(ModItems.GIANT_GOBLIN_SPAWN_EGG.get());
                output.accept(ModItems.CRAZY_GOBLIN_SPAWN_EGG.get());
                output.accept(ModItems.GOBLIN_TRADER_SPAWN_EGG.get());
                output.accept(ModItems.GOBLIN_ELDER_SPAWN_EGG.get());
                output.accept(ModItems.GOBLIN_SHAMAN_SPAWN_EGG.get());
                output.accept(ModItems.GOBLIN_KING_SPAWN_EGG.get());
                output.accept(ModItems.GOBLIN_CHILD_SPAWN_EGG.get());
                output.accept(ModItems.GOBLIN_VILLAGER_SPAWN_EGG.get());
                output.accept(ModItems.GOBLIN_SAGE_SPAWN_EGG.get());
                output.accept(ModItems.GOBLIN_HEADMAN_SPAWN_EGG.get());
                output.accept(ModItems.BUILDING_SITE.get());
                output.accept(ModItems.STOREHOUSE_CRATE.get());
            })
            .build());

    private ModCreativeTabs() {
    }
}
