package com.goblinraiders.entity.goal;

import com.goblinraiders.entity.AbstractGoblin;
import java.util.EnumSet;
import javax.annotation.Nullable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

/**
 * A hired goblin fights for its employer: it goes after whatever hurts the employer, whatever the employer hits
 * (goblins included) and any mob that is about to attack the employer. Never the employer and never the employer's
 * other mercenaries - those are its allies, see {@link AbstractGoblin#isAlliedTo}.
 */
public class HirerDefendGoal extends Goal {
    /** Mobs going for the employer within this range get attacked before they strike. */
    private static final double THREAT_RANGE = 16.0;
    private final AbstractGoblin goblin;
    @Nullable
    private LivingEntity enemy;
    private int lastHurtBy;
    private int lastHurt;

    public HirerDefendGoal(AbstractGoblin goblin) {
        this.goblin = goblin;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        Player hirer = this.goblin.getHirer();
        if (hirer == null) {
            return false;
        }
        LivingEntity attacker = hirer.getLastHurtByMob();
        if (attacker != null && hirer.getLastHurtByMobTimestamp() != this.lastHurtBy) {
            this.lastHurtBy = hirer.getLastHurtByMobTimestamp();
            if (this.fair(attacker)) {
                this.enemy = attacker;
                return true;
            }
        }
        LivingEntity victim = hirer.getLastHurtMob();
        if (victim != null && hirer.getLastHurtMobTimestamp() != this.lastHurt) {
            this.lastHurt = hirer.getLastHurtMobTimestamp();
            if (this.fair(victim)) {
                this.enemy = victim;
                return true;
            }
        }
        LivingEntity current = this.goblin.getTarget();
        if ((current == null || !current.isAlive()) && this.goblin.tickCount % 10 == 0) {
            this.enemy = this.threatTo(hirer);
            return this.enemy != null;
        }
        return false;
    }

    /** The nearest mob that has the employer as its target. */
    @Nullable
    private LivingEntity threatTo(Player hirer) {
        Mob nearest = null;
        double best = Double.MAX_VALUE;
        for (Mob mob : this.goblin.level().getEntitiesOfClass(Mob.class, hirer.getBoundingBox().inflate(THREAT_RANGE),
                m -> m.getTarget() == hirer && this.fair(m))) {
            double dist = mob.distanceToSqr(this.goblin);
            if (dist < best) {
                best = dist;
                nearest = mob;
            }
        }
        return nearest;
    }

    private boolean fair(LivingEntity target) {
        return target != this.goblin && target.isAlive() && TargetingConditions.forCombat().ignoreLineOfSight().test(this.goblin, target);
    }

    @Override
    public boolean canContinueToUse() {
        return false;   // only picks the target; the attack goals take it from there
    }

    @Override
    public void start() {
        this.goblin.setTarget(this.enemy);
        this.enemy = null;
    }
}
