package com.goblinraiders.entity.goal;

import com.goblinraiders.entity.AbstractGoblin;
import com.goblinraiders.entity.Speech;
import java.util.EnumSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Two idle goblins standing next to each other, with nothing between them, have a little conversation: they stop,
 * face each other and take turns with speech bubbles. The goblin that starts it drives the lines; the other one runs
 * this goal in listener mode. Walking apart or a wall between them ends it.
 */
public class GoblinChatGoal extends Goal {
    /** Goblins only chat when standing right next to each other. */
    private static final double RANGE = 2.5;
    /** The conversation breaks off when they end up further apart than this. */
    private static final double KEEP_RANGE_SQR = 3.5 * 3.5;
    /** Ticks between two lines. */
    private static final int LINE_GAP = 55;

    private final AbstractGoblin goblin;
    @Nullable
    private AbstractGoblin partner;
    private boolean leader;
    private int conversation;
    private int line;
    private int timer;

    public GoblinChatGoal(AbstractGoblin goblin) {
        this.goblin = goblin;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (this.goblin.chatPartner != null) {
            // Someone started a conversation with us.
            this.partner = this.goblin.chatPartner;
            this.leader = false;
            return this.partner.isAlive();
        }
        if (!this.goblin.canChat() || this.goblin.getRandom().nextInt(500) != 0) {
            return false;
        }
        List<AbstractGoblin> around = this.goblin.level().getEntitiesOfClass(AbstractGoblin.class,
                this.goblin.getBoundingBox().inflate(RANGE, 1.0, RANGE),
                other -> other != this.goblin && other.canChat() && canTalk(this.goblin, other));
        if (around.isEmpty()) {
            return false;
        }
        this.partner = around.get(this.goblin.getRandom().nextInt(around.size()));
        this.leader = true;
        return true;
    }

    /** Two goblins can start a conversation only next to each other with nothing in between. */
    public static boolean canTalk(AbstractGoblin a, AbstractGoblin b) {
        return a.distanceToSqr(b) <= RANGE * RANGE && Math.abs(a.getY() - b.getY()) < 1.0 && a.hasLineOfSight(b);
    }

    @Override
    public void start() {
        if (this.leader) {
            this.goblin.chatPartner = this.partner;
            this.partner.chatPartner = this.goblin;
            this.conversation = this.goblin.getRandom().nextInt(Speech.CONVERSATIONS.length);
            this.line = 0;
            this.timer = 10;
        }
        this.goblin.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        return this.partner != null && this.partner.isAlive() && this.goblin.getTarget() == null && this.partner.getTarget() == null
                && this.goblin.distanceToSqr(this.partner) < KEEP_RANGE_SQR && this.goblin.getSensing().hasLineOfSight(this.partner)
                && this.goblin.chatPartner == this.partner
                && (!this.leader || this.line < Speech.CONVERSATIONS[this.conversation] || this.timer > 0);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        if (this.partner == null) {
            return;
        }
        this.goblin.getNavigation().stop();
        this.goblin.getLookControl().setLookAt(this.partner, 30.0F, 30.0F);
        if (!this.leader || --this.timer > 0) {
            return;
        }
        if (this.line < Speech.CONVERSATIONS[this.conversation]) {
            AbstractGoblin speaker = this.line % 2 == 0 ? this.goblin : this.partner;
            speaker.say(Speech.conversationLine(this.conversation, this.line));
            this.line++;
            this.timer = LINE_GAP;
        }
    }

    @Override
    public void stop() {
        if (this.leader && this.partner != null && this.partner.chatPartner == this.goblin) {
            this.partner.chatPartner = null;
        }
        this.goblin.chatPartner = null;
        this.partner = null;
    }
}
