package com.goblinraiders.entity.goal;

import com.goblinraiders.entity.AbstractGoblin;
import com.goblinraiders.raid.Caravans;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Caravan goblins walk home: every couple of seconds they pick a waypoint a dozen blocks closer to their village.
 * If they get stuck (water, cliffs) they hop the gap rather than stand there forever.
 */
public class CaravanGoal extends Goal {
    private static final double STEP = 12.0;
    private final AbstractGoblin goblin;
    private int repath;
    private int stuckTicks;
    @Nullable
    private Vec3 lastPos;

    public CaravanGoal(AbstractGoblin goblin) {
        this.goblin = goblin;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return this.goblin.getCaravanId() != null && this.goblin.getTarget() == null && Caravans.destinationOf(this.goblin) != null;
    }

    @Override
    public void start() {
        this.repath = 0;
        this.stuckTicks = 0;
        this.lastPos = this.goblin.position();
    }

    @Override
    public void tick() {
        BlockPos village = Caravans.destinationOf(this.goblin);
        if (village == null) {
            return;
        }
        if (--this.repath > 0) {
            return;
        }
        this.repath = 40;
        Vec3 pos = this.goblin.position();
        if (this.lastPos != null && pos.distanceToSqr(this.lastPos) < 2.0) {
            this.stuckTicks += 40;
        } else {
            this.stuckTicks = 0;
        }
        this.lastPos = pos;
        double dx = village.getX() + 0.5 - pos.x, dz = village.getZ() + 0.5 - pos.z;
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 1.0) {
            return;
        }
        double step = Math.min(STEP, dist);
        int wx = (int) Math.floor(pos.x + dx / dist * step), wz = (int) Math.floor(pos.z + dz / dist * step);
        int wy = this.goblin.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, wx, wz);
        if (this.stuckTicks >= 120) {
            // Stuck for six seconds: hop forward.
            int hx = (int) Math.floor(pos.x + dx / dist * 6), hz = (int) Math.floor(pos.z + dz / dist * 6);
            int hy = this.goblin.level().getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, hx, hz);
            this.goblin.teleportTo(hx + 0.5, hy, hz + 0.5);
            this.stuckTicks = 0;
            return;
        }
        this.goblin.getNavigation().moveTo(wx + 0.5, wy, wz + 0.5, 1.0);
    }
}
