package com.goblinraiders.entity.goal;

import com.goblinraiders.entity.AbstractGoblin;
import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

/** While a player talks to the goblin through a dialog window (or trades), it stands still and looks at them. */
public class GoblinTalkGoal extends Goal {
    private final AbstractGoblin goblin;

    public GoblinTalkGoal(AbstractGoblin goblin) {
        this.goblin = goblin;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        return this.goblin.getTalkingTo() != null && this.goblin.getTarget() == null;
    }

    @Override
    public void start() {
        this.goblin.getNavigation().stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Player player = this.goblin.getTalkingTo();
        if (player != null) {
            this.goblin.getNavigation().stop();
            this.goblin.getLookControl().setLookAt(player, 30.0F, 30.0F);
        }
    }
}
