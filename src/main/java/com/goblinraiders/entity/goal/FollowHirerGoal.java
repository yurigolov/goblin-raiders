package com.goblinraiders.entity.goal;

import com.goblinraiders.entity.AbstractGoblin;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/** A hired goblin (or a worker child) keeps up with its player, and catches up by teleporting when left far behind. */
public class FollowHirerGoal extends Goal {
    private final AbstractGoblin goblin;
    private int repath;

    public FollowHirerGoal(AbstractGoblin goblin) {
        this.goblin = goblin;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Player hirer = this.goblin.getLeader();
        return hirer != null && this.goblin.getTarget() == null && this.goblin.distanceToSqr(hirer) > 25.0;
    }

    @Override
    public boolean canContinueToUse() {
        Player hirer = this.goblin.getLeader();
        return hirer != null && this.goblin.getTarget() == null && this.goblin.distanceToSqr(hirer) > 9.0;
    }

    @Override
    public void tick() {
        Player hirer = this.goblin.getLeader();
        if (hirer == null) {
            return;
        }
        this.goblin.getLookControl().setLookAt(hirer, 10.0F, this.goblin.getMaxHeadXRot());
        if (this.goblin.distanceToSqr(hirer) > 30.0 * 30.0 && hirer.onGround()) {
            this.goblin.teleportTo(hirer.getX(), hirer.getY(), hirer.getZ());
            this.goblin.getNavigation().stop();
            return;
        }
        if (--this.repath <= 0) {
            this.repath = 10;
            this.goblin.getNavigation().moveTo(hirer, 1.2);
        }
    }

    @Override
    public void stop() {
        this.goblin.getNavigation().stop();
    }
}
