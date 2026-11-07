package com.goblinraiders.entity.goal;

import com.goblinraiders.entity.AbstractGoblin;
import com.goblinraiders.entity.GoblinNight;
import com.goblinraiders.entity.Speech;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * At night a watchman stands by the campfire nearest to its home, looks around, now and then walks a round and comes
 * back. Spotting an intruder (much closer if they sneak, only in sight) raises the alarm.
 */
public class WatchGoal extends Goal {
    private static final int POST_SEARCH_RADIUS = 20;
    private final AbstractGoblin goblin;
    @Nullable
    private BlockPos fire;
    @Nullable
    private Vec3 spot;
    private boolean onRound;
    private int waitTicks;

    public WatchGoal(AbstractGoblin goblin) {
        this.goblin = goblin;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return this.goblin.isWatchman() && GoblinNight.isNight(this.goblin.level()) && this.goblin.getTarget() == null
                && this.goblin.getTalkingTo() == null;
    }

    @Override
    public void start() {
        BlockPos home = this.goblin.getHome();
        this.fire = home == null ? null
                : GoblinNight.nearestCampfire(this.goblin.level(), home, POST_SEARCH_RADIUS, this.goblin.blockPosition());
        this.goToPost();
    }

    /** A spot two or three blocks from the fire (or home, if the village has no campfire). */
    private void goToPost() {
        BlockPos center = this.fire != null ? this.fire : this.goblin.getHome();
        if (center == null) {
            this.spot = null;
            return;
        }
        double angle = this.goblin.getRandom().nextDouble() * Math.PI * 2.0;
        double r = this.fire != null ? 2.5 : 1.0;
        this.spot = new Vec3(center.getX() + 0.5 + Math.cos(angle) * r, center.getY(), center.getZ() + 0.5 + Math.sin(angle) * r);
        this.onRound = false;
        this.waitTicks = 200 + this.goblin.getRandom().nextInt(400);
        this.goblin.getNavigation().moveTo(this.spot.x, this.spot.y, this.spot.z, 0.8);
    }

    /** A walk to somewhere nearby, then back to the post. */
    private void goOnRound() {
        Vec3 target = LandRandomPos.getPos(this.goblin, 10, 4);
        if (target == null) {
            this.waitTicks = 100;
            return;
        }
        this.spot = target;
        this.onRound = true;
        this.waitTicks = 40 + this.goblin.getRandom().nextInt(60);
        this.goblin.getNavigation().moveTo(target.x, target.y, target.z, 0.7);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.goblin.tickCount % 10 == 0) {
            Player intruder = GoblinNight.spotIntruder(this.goblin);
            if (intruder != null) {
                this.goblin.raiseAlarm(intruder);
                this.goblin.getLookControl().setLookAt(intruder);
                return;
            }
        }
        if (this.spot == null) {
            return;
        }
        if (this.goblin.getNavigation().isDone()) {
            if (this.fire != null && !this.onRound) {
                this.goblin.getLookControl().setLookAt(this.fire.getX() + 0.5, this.fire.getY() + 0.5, this.fire.getZ() + 0.5);
            }
            if (this.goblin.getRandom().nextInt(80) == 0) {
                double angle = this.goblin.getRandom().nextDouble() * Math.PI * 2.0;
                this.goblin.getLookControl().setLookAt(this.goblin.getX() + Math.cos(angle) * 8.0, this.goblin.getEyeY(),
                        this.goblin.getZ() + Math.sin(angle) * 8.0);
            }
            if (--this.waitTicks <= 0) {
                if (this.onRound) {
                    this.goToPost();
                } else {
                    this.goOnRound();
                }
            }
        }
        if (this.goblin.getRandom().nextInt(900) == 0 && !this.goblin.isSpeaking()) {
            this.goblin.sayRandom(Speech.WATCH);
        }
    }

    @Override
    public void stop() {
        this.goblin.getNavigation().stop();
        this.spot = null;
    }
}
