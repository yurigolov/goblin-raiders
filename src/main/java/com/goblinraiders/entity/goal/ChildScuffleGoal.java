package com.goblinraiders.entity.goal;

import com.goblinraiders.entity.GoblinChild;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Now and then two idle goblin children start a scuffle: they go at each other with their fists until one of them has
 * had enough (half its health) or they get bored.
 */
public class ChildScuffleGoal extends Goal {
    private static final int MAX_TICKS = 200;
    private final GoblinChild child;
    @Nullable
    private GoblinChild rival;
    private int ticks;

    public ChildScuffleGoal(GoblinChild child) {
        this.child = child;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    /** Whether {@code child} is free for a scuffle: a village child, awake, not busy, not beaten up. */
    public static boolean idle(GoblinChild child) {
        return child.isAlive() && child.getOwnerId() == null && !child.isDozing() && child.getTarget() == null
                && child.getTalkingTo() == null && child.getHealth() > child.getMaxHealth() * 0.5F;
    }

    @Override
    public boolean canUse() {
        if (this.child.getRandom().nextInt(2000) != 0 || !idle(this.child)) {
            return false;
        }
        List<GoblinChild> others = this.child.level().getEntitiesOfClass(GoblinChild.class, this.child.getBoundingBox().inflate(8.0),
                other -> other != this.child && idle(other) && this.child.hasLineOfSight(other));
        if (others.isEmpty()) {
            return false;
        }
        this.rival = others.get(this.child.getRandom().nextInt(others.size()));
        return true;
    }

    @Override
    public void start() {
        start(this.child, this.rival);
        this.ticks = 0;
    }

    /** The two children go at each other. */
    public static void start(GoblinChild a, GoblinChild b) {
        a.setTarget(b);
        b.setTarget(a);
        a.sayRandom("play");
    }

    @Override
    public boolean canContinueToUse() {
        return this.rival != null && this.rival.isAlive() && this.child.getTarget() == this.rival && ++this.ticks < MAX_TICKS
                && this.child.getHealth() > this.child.getMaxHealth() * 0.5F && this.rival.getHealth() > this.rival.getMaxHealth() * 0.5F;
    }

    @Override
    public void stop() {
        if (this.rival != null && this.rival.getTarget() == this.child) {
            this.rival.setTarget(null);
        }
        if (this.child.getTarget() == this.rival) {
            this.child.setTarget(null);
        }
        this.rival = null;
    }
}
