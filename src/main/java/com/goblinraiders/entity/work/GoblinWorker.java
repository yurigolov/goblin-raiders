package com.goblinraiders.entity.work;

import com.goblinraiders.entity.AbstractGoblin;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;

/**
 * A goblin that works: worker children bought from the king and the settlers of peaceful villages.
 * {@link WorkGoal} runs the job; {@link WorkSkills} holds the know-how.
 */
public interface GoblinWorker {
    AbstractGoblin asGoblin();

    WorkJob getJob();

    /** Where it works (null: nowhere right now). */
    @Nullable
    BlockPos getWorkplace();

    /** How far from the workplace it looks for fields, trees, chests. */
    int workRadius();

    /**
     * How far from the workplace the common storehouse may be: settlers carry everything to the storehouse crates of
     * their village (and fetch materials from there). 0: no storehouse, the nearest chest around the workplace serves.
     */
    default int storeRange() {
        return 0;
    }

    /** What it carries (nine slots). */
    SimpleContainer getPocket();

    /** Whether it should work now (has a job and a workplace, is not following anyone). */
    boolean isWorking();

    /** A bubble about what is wrong, now and then. */
    void complain(String topic);

    /** Called when it puts something into a chest (advancements for the owner of a worker child). */
    default void onDelivered(ItemStack stack) {
    }

    /** Called for everything a miner digs up. */
    default void onFound(ItemStack stack) {
    }
}
