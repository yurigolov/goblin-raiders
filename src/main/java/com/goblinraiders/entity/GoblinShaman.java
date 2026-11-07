package com.goblinraiders.entity;

import com.goblinraiders.registry.ModEffects;
import com.goblinraiders.registry.ModSounds;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Skin 9: the goblin shaman lives in the shaman's hut. In a fight he throws potions, gives fighting goblins
 * strength and speed, heals giants and curses players (see {@link com.goblinraiders.effect.GoblinCurseEffect}),
 * muttering strange spells all the while.
 */
public class GoblinShaman extends AbstractGoblin implements RangedAttackMob {
    public static final String SPELL = "spell";
    private static final int BUFF_COOLDOWN = 240;
    private static final int HEAL_COOLDOWN = 100;
    private static final int CURSE_COOLDOWN = 400;
    private static final int CURSE_TICKS = 400;

    private int buffCooldown = 60;
    private int healCooldown = 20;
    private int curseCooldown = 100;

    public GoblinShaman(EntityType<? extends GoblinShaman> type, Level level) {
        super(type, level);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return goblinAttributes()
                .add(Attributes.MAX_HEALTH, 22.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void registerCombatGoals() {
        this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0, 50, 10.0F));
        this.addHostileTargets();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.healCooldown-- <= 0) {
            this.healCooldown = this.healGiant() ? HEAL_COOLDOWN : 20;
        }
        if (this.buffCooldown-- <= 0) {
            this.buffCooldown = this.buffAllies() ? BUFF_COOLDOWN : 40;
        }
        if (this.getTarget() instanceof Player player && this.curseCooldown-- <= 0) {
            this.curseCooldown = this.curse(player) ? CURSE_COOLDOWN : 40;
        }
    }

    /** Heals a hurt giant goblin nearby. */
    public boolean healGiant() {
        List<GiantGoblin> giants = this.level().getEntitiesOfClass(GiantGoblin.class, this.getBoundingBox().inflate(16.0),
                g -> g.isAlive() && this.isAlliedTo(g) && g.getHealth() < g.getMaxHealth() * 0.7F && this.hasLineOfSight(g));
        if (giants.isEmpty()) {
            return false;
        }
        GiantGoblin giant = giants.get(0);
        giant.heal(16.0F);
        giant.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
        this.cast(giant, ParticleTypes.HEART);
        return true;
    }

    /** Strength and speed for every goblin around that is fighting. */
    public boolean buffAllies() {
        List<AbstractGoblin> fighting = this.level().getEntitiesOfClass(AbstractGoblin.class, this.getBoundingBox().inflate(10.0),
                g -> g != this && g.isAlive() && this.isAlliedTo(g) && g.getTarget() != null);
        if (fighting.isEmpty()) {
            return false;
        }
        for (AbstractGoblin goblin : fighting.subList(0, Math.min(5, fighting.size()))) {
            goblin.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 200, 0));
            goblin.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 0));
            this.particles(goblin, ParticleTypes.HAPPY_VILLAGER);
        }
        this.cast(this, ParticleTypes.WITCH);
        return true;
    }

    /** Puts the goblin curse on a player in sight. */
    public boolean curse(Player player) {
        if (this.distanceToSqr(player) > 16.0 * 16.0 || !this.hasLineOfSight(player) || player.isCreative() || player.isSpectator()) {
            return false;
        }
        player.addEffect(new MobEffectInstance(ModEffects.CURSE, CURSE_TICKS, 0), this);
        this.cast(player, ParticleTypes.WITCH);
        return true;
    }

    private void cast(Entity at, ParticleOptions particle) {
        this.sayRandom(SPELL);
        this.playSound(ModSounds.SHAMAN_SPELL.get(), 1.0F, this.getVoicePitch());
        this.particles(at, particle);
        this.swing(this.getUsedItemHand());
    }

    private void particles(Entity at, ParticleOptions particle) {
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(particle, at.getX(), at.getY() + at.getBbHeight() * 0.7, at.getZ(), 10, at.getBbWidth() * 0.5,
                    at.getBbHeight() * 0.3, at.getBbWidth() * 0.5, 0.05);
        }
    }

    /** Throws a splash potion, picked the way witches pick theirs. */
    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        Vec3 motion = target.getDeltaMovement();
        double dx = target.getX() + motion.x - this.getX();
        double dy = target.getEyeY() - 1.1 - this.getY();
        double dz = target.getZ() + motion.z - this.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        Holder<Potion> potion = Potions.HARMING;
        if (dist >= 8.0 && !target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            potion = Potions.SLOWNESS;
        } else if (target.getHealth() >= 8.0F && !target.hasEffect(MobEffects.POISON)) {
            potion = Potions.POISON;
        } else if (dist <= 3.0 && !target.hasEffect(MobEffects.WEAKNESS) && this.random.nextFloat() < 0.25F) {
            potion = Potions.WEAKNESS;
        }
        ThrownPotion thrown = new ThrownPotion(this.level(), this);
        thrown.setItem(PotionContents.createItemStack(Items.SPLASH_POTION, potion));
        thrown.setXRot(thrown.getXRot() + 20.0F);
        thrown.shoot(dx, dy + dist * 0.2, dz, 0.75F, 8.0F);
        this.playSound(SoundEvents.WITCH_THROW, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
        this.level().addFreshEntity(thrown);
    }

    /** Night is the time for rituals: shamans stay up and keep watch. */
    @Override
    protected int sleepersPercent() {
        return 0;
    }

    @Override
    public String voice() {
        return "shaman";
    }

    @Override
    protected int idleLineChance() {
        return 900;
    }

    @Override
    protected float pitchFactor() {
        return 1.05F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.SHAMAN_AMBIENT.get();
    }
}
