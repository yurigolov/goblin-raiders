package com.goblinraiders.registry;

import com.goblinraiders.GoblinRaiders;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> REGISTER = DeferredRegister.create(Registries.ARMOR_MATERIAL, GoblinRaiders.MODID);

    /** Same numbers as diamond armor. Textures: textures/models/armor/goblin_layer_1.png and _2.png. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GOBLIN = REGISTER.register("goblin", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 6);
                map.put(ArmorItem.Type.CHESTPLATE, 8);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 11);
            }),
            10,
            SoundEvents.ARMOR_EQUIP_DIAMOND,
            () -> Ingredient.of(Items.DIAMOND),
            List.of(new ArmorMaterial.Layer(GoblinRaiders.id("goblin"))),
            2.0F,
            0.0F));

    /** Durability multiplier of diamond armor. */
    public static final int DURABILITY_FACTOR = 33;

    /** The goblin king's crown: netherite helmet numbers. Texture: textures/models/armor/goblin_crown_layer_1.png. */
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> GOBLIN_CROWN = REGISTER.register("goblin_crown", () -> new ArmorMaterial(
            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                map.put(ArmorItem.Type.BOOTS, 3);
                map.put(ArmorItem.Type.LEGGINGS, 6);
                map.put(ArmorItem.Type.CHESTPLATE, 8);
                map.put(ArmorItem.Type.HELMET, 3);
                map.put(ArmorItem.Type.BODY, 11);
            }),
            15,
            SoundEvents.ARMOR_EQUIP_GOLD,
            () -> Ingredient.of(Items.GOLD_INGOT),
            List.of(new ArmorMaterial.Layer(GoblinRaiders.id("goblin_crown"))),
            3.0F,
            0.1F));

    /** Durability multiplier of netherite armor. */
    public static final int CROWN_DURABILITY_FACTOR = 37;

    private ModArmorMaterials() {
    }
}
