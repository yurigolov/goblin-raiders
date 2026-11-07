package com.goblinraiders.entity.goal;

import com.goblinraiders.entity.AbstractGoblin;
import com.goblinraiders.entity.GoblinNight;
import com.goblinraiders.entity.Speech;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/**
 * At night a sleeper goes home (where it was placed with its village) and dozes off. Asleep it notices nothing,
 * except a player who walks by without sneaking - then it wakes up and raises the alarm. Morning wakes it too.
 */
public class SleepGoal extends Goal {
    /** If the way home is blocked, the goblin lies down where it is after this long. */
    private static final int MAX_WALK_TICKS = 400;
    private final AbstractGoblin goblin;
    private int walkTicks;

    public SleepGoal(AbstractGoblin goblin) {
        this.goblin = goblin;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.goblin.canDozeOff();
    }

    @Override
    public boolean canContinueToUse() {
        return this.goblin.isDozing() ? GoblinNight.isNight(this.goblin.level()) && this.goblin.getTarget() == null : this.goblin.canDozeOff();
    }

    @Override
    public void start() {
        this.walkTicks = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.goblin.isDozing()) {
            if (this.goblin.tickCount % 10 == 0) {
                this.listen();
            }
            if (this.goblin.getRandom().nextInt(400) == 0) {
                this.goblin.mumble(Speech.SLEEP);
            }
            return;
        }
        BlockPos home = this.goblin.getHome();
        if (home == null || ++this.walkTicks > MAX_WALK_TICKS || this.goblin.blockPosition().distSqr(home) <= 2.0) {
            this.goblin.fallAsleep();
        } else if (this.walkTicks % 20 == 1) {
            this.goblin.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 0.8);
        }
    }

    /** A player stomping around close by wakes the goblin; one who sneaks does not. */
    private void listen() {
        double range = GoblinNight.HEARING_RANGE;
        for (Player player : this.goblin.level().getEntitiesOfClass(Player.class, this.goblin.getBoundingBox().inflate(range))) {
            if (!GoblinNight.isIntruder(player) || player.isCrouching() || this.goblin.isFamiliar(player)) {
                continue;
            }
            double dist = this.goblin.distanceToSqr(player);
            if (dist < 9.0 || player.isSprinting() || this.goblin.getRandom().nextInt(3) == 0) {
                this.goblin.wakeUp(true);
                this.goblin.raiseAlarm(player);
                return;
            }
        }
    }

    @Override
    public void stop() {
        if (this.goblin.isDozing()) {
            this.goblin.wakeUp(false);   // morning
            if (this.goblin.getRandom().nextInt(3) == 0) {
                this.goblin.sayRandom(Speech.MORNING);
            }
        }
        this.goblin.getNavigation().stop();
    }
}
