package com.goblinraiders.entity.work;

import java.util.Locale;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** What a working goblin does all day (see {@link WorkGoal}). */
public enum WorkJob {
    NONE(Items.AIR),
    FARMER(Items.IRON_HOE),
    MINER(Items.IRON_PICKAXE),
    LUMBERJACK(Items.IRON_AXE),
    BUILDER(Items.IRON_SHOVEL),
    COOK(Items.BOWL);

    private final Item tool;

    WorkJob(Item tool) {
        this.tool = tool;
    }

    public ItemStack tool() {
        return new ItemStack(this.tool);
    }

    /** Lower-case name, used in speech and translation keys. */
    public String key() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public static WorkJob byId(int id) {
        return id >= 0 && id < values().length ? values()[id] : NONE;
    }
}
