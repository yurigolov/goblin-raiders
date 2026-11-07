package com.goblinraiders.entity;

import com.goblinraiders.advancement.GoblinAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Skin 3: ordinary stats, only fights back (or when other goblins call for help). Talks to anybody who comes in peace,
 * not only to those in goblin shape (see {@link AbstractGoblin#talksWith}).
 */
public class NeutralGoblin extends AbstractGoblin {
    public NeutralGoblin(EntityType<? extends NeutralGoblin> type, Level level) {
        super(type, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return goblinAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 3.0);
    }

    @Override
    protected void registerCombatGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
    }

    @Override
    public boolean talksWith(Player player) {
        return true;
    }

    @Override
    protected void onTalkStarted(ServerPlayer player) {
        GoblinAdvancements.award(player, GoblinAdvancements.NEUTRAL_TALK);
    }

    @Override
    public boolean isPreventingPlayerRest(Player player) {
        return this.getTarget() == player;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected int idleLineChance() {
        return 1000;
    }

    @Override
    public String voice() {
        return "neutral";
    }

    @Override
    protected float pitchFactor() {
        return 1.1F;
    }
}
