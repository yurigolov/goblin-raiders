package com.goblinraiders.entity;

import com.goblinraiders.entity.goal.SleepGoal;
import com.goblinraiders.entity.goal.WatchGoal;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * The day rhythm of goblin villages. At night most villagers sleep ({@link SleepGoal}) - a player who sneaks can get
 * past them - while the rest keep watch by the campfires ({@link WatchGoal}). Goblins light the village campfires at
 * dusk and put them out in the morning.
 */
public final class GoblinNight {
    /** How far a watchman sees an intruder, and how far if the intruder sneaks. */
    public static final double SPOT_RANGE = 16.0;
    public static final double SPOT_RANGE_SNEAKING = 6.0;
    /** Sleepers within this range may hear a player who walks by without sneaking. */
    public static final double HEARING_RANGE = 6.0;
    /** An alarm wakes every goblin this close. */
    public static final double ALARM_RANGE = 16.0;
    /** How close a goblin passes by a campfire to light it or put it out. */
    private static final int FIRE_REACH = 4;

    public static boolean isNight(Level level) {
        if (level.dimensionType().hasFixedTime()) {
            return false;
        }
        long time = level.getDayTime() % 24000L;
        return time >= 13000L && time < 23000L;
    }

    /** A player the goblins treat as a stranger to be chased off (not a friend, not in creative or spectator mode). */
    public static boolean isIntruder(Player player) {
        return player.isAlive() && !player.isSpectator() && !player.isCreative() && !GoblinFriends.isFriend(player);
    }

    /** Whether {@code goblin} on watch notices {@code player} now. */
    public static boolean spots(AbstractGoblin goblin, Player player) {
        if (!isIntruder(player)) {
            return false;
        }
        double range = player.isInvisible() ? 3.0 : player.isCrouching() ? SPOT_RANGE_SNEAKING : SPOT_RANGE;
        return goblin.distanceToSqr(player) <= range * range && goblin.hasLineOfSight(player);
    }

    /** The intruder a watchman sees, if any. */
    @Nullable
    public static Player spotIntruder(AbstractGoblin goblin) {
        for (Player player : goblin.level().players()) {
            if (spots(goblin, player)) {
                return player;
            }
        }
        return null;
    }

    /**
     * Lights one unlit campfire within reach at night, or puts out a burning one by day.
     * Returns whether it changed one.
     */
    public static boolean tendFire(AbstractGoblin goblin, boolean night) {
        Level level = goblin.level();
        BlockPos at = goblin.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-FIRE_REACH, -1, -FIRE_REACH), at.offset(FIRE_REACH, 2, FIRE_REACH))) {
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.CAMPFIRES) || !state.hasProperty(CampfireBlock.LIT)) {
                continue;
            }
            boolean lit = state.getValue(CampfireBlock.LIT);
            if (night && CampfireBlock.canLight(state)) {
                level.setBlock(pos, state.setValue(CampfireBlock.LIT, true), 11);
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 0.9F);
            } else if (!night && lit) {
                level.setBlock(pos, state.setValue(CampfireBlock.LIT, false), 11);
                level.levelEvent(null, LevelEvent.SOUND_EXTINGUISH_FIRE, pos, 0);
            } else {
                continue;
            }
            level.gameEvent(goblin, GameEvent.BLOCK_CHANGE, pos);
            goblin.swing(InteractionHand.MAIN_HAND);
            return true;
        }
        return false;
    }

    /** The campfire nearest to {@code from} within {@code radius} blocks of {@code center}, or null. */
    @Nullable
    public static BlockPos nearestCampfire(Level level, BlockPos center, int radius, BlockPos from) {
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 4, radius))) {
            if (level.getBlockState(pos).is(BlockTags.CAMPFIRES)) {
                double dist = pos.distSqr(from);
                if (dist < bestDist) {
                    bestDist = dist;
                    best = pos.immutable();
                }
            }
        }
        return best;
    }

    private GoblinNight() {
    }
}
