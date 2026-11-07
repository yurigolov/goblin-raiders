package com.goblinraiders.entity;

import com.goblinraiders.network.ModNetwork;
import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.network.OpenDialogPayload;
import com.goblinraiders.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Skin 6: a small, old and completely peaceful goblin. Talking to him opens a dialog with stories about the goblins.
 * He never fights; when hurt he runs and the goblins around come to his aid.
 */
public class GoblinElder extends AbstractGoblin {
    private static final int TALK_TICKS = 20 * 60;

    public GoblinElder(EntityType<? extends GoblinElder> type, Level level) {
        super(type, level);
        this.xpReward = 2;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return goblinAttributes()
                .add(Attributes.MAX_HEALTH, 16.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override
    protected void registerCombatGoals() {
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.3));
    }

    @Override
    protected boolean fightsBack() {
        return false;
    }

    /** Old bones: the elder always sleeps at night. */
    @Override
    protected int sleepersPercent() {
        return 100;
    }

    @Override
    protected boolean keepsWatch() {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || player.getItemInHand(hand).is(Items.NAME_TAG) || !this.isAlive()) {
            return super.mobInteract(player, hand);
        }
        if (this.wakeIfDozing()) {
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            this.startTalking(player, TALK_TICKS);
            ModNetwork.send(serverPlayer, new OpenDialogPayload(this.getId(), new MerchantOffers()));
            GoblinAdvancements.award(serverPlayer, GoblinAdvancements.ELDER);
            this.playSound(ModSounds.ELDER_TALK.get(), this.getSoundVolume(), this.getVoicePitch());
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && source.getEntity() instanceof LivingEntity attacker && !this.isAlliedTo(attacker)) {
            this.stopTalking();
            this.callForHelp(attacker, 20.0);
            if (!this.isSpeaking()) {
                this.sayRandom(Speech.HELP);
            }
        }
        return hurt;
    }

    @Override
    public void setTarget(LivingEntity target) {
        // Never goes after anyone.
    }

    @Override
    public boolean isPreventingPlayerRest(Player player) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected int idleLineChance() {
        return 1200;
    }

    /** The elder keeps close to his hut. */
    @Override
    protected int homeRadius() {
        return 10;
    }

    @Override
    public String voice() {
        return "elder";
    }

    @Override
    protected float pitchFactor() {
        return 0.85F;
    }

    @Override
    protected SoundEvent getTalkSound() {
        return ModSounds.ELDER_TALK.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.ELDER_AMBIENT.get();
    }
}
