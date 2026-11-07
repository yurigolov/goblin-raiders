package com.goblinraiders.entity;

import com.goblinraiders.registry.ModItems;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

/**
 * Skin 1: about 1.5 blocks tall, weak but very fast. One in twenty carries a goblin bow and shoots from afar,
 * one in twenty a goblin dagger.
 */
public class SmallGoblin extends AbstractGoblin implements RangedAttackMob {
    private static final float BOW_CHANCE = 0.05F;
    private static final float DAGGER_CHANCE = 0.05F;

    private final RangedBowAttackGoal<SmallGoblin> bowGoal = new RangedBowAttackGoal<>(this, 1.0, 20, 15.0F);
    private final MeleeAttackGoal meleeGoal = new MeleeAttackGoal(this, 1.1, false);

    public SmallGoblin(EntityType<? extends SmallGoblin> type, Level level) {
        super(type, level);
        this.xpReward = 4;
        this.reassessWeaponGoal();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return goblinAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.36)
                .add(Attributes.ATTACK_DAMAGE, 2.0);
    }

    @Override
    protected void registerCombatGoals() {
        // The attack goal (bow or melee) is picked by reassessWeaponGoal, like skeletons do.
        this.addHostileTargets();
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        float roll = random.nextFloat();
        if (roll < BOW_CHANCE) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GOBLIN_BOW.get()));
        } else if (roll < BOW_CHANCE + DAGGER_CHANCE) {
            this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GOBLIN_DAGGER.get()));
        }
        this.setDropChance(EquipmentSlot.MAINHAND, 0.05F);
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.reassessWeaponGoal();
        return data;
    }

    /** Bow in hand: keep distance and shoot; anything else: run in and bite. */
    public void reassessWeaponGoal() {
        if (this.level() == null || this.level().isClientSide) {
            return;
        }
        this.goalSelector.removeGoal(this.meleeGoal);
        this.goalSelector.removeGoal(this.bowGoal);
        if (this.isHolding(stack -> stack.getItem() instanceof BowItem)) {
            this.goalSelector.addGoal(2, this.bowGoal);
        } else {
            this.goalSelector.addGoal(2, this.meleeGoal);
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {
        super.setItemSlot(slot, stack);
        if (slot == EquipmentSlot.MAINHAND && this.bowGoal != null) {
            this.reassessWeaponGoal();
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.reassessWeaponGoal();
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        ItemStack weapon = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, item -> item instanceof BowItem));
        ItemStack ammo = this.getProjectile(weapon);
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammo, distanceFactor, weapon);
        if (weapon.getItem() instanceof ProjectileWeaponItem bow) {
            arrow = bow.customArrow(arrow, ammo, weapon);
        }
        double dx = target.getX() - this.getX();
        double dy = target.getY(1.0 / 3.0) - arrow.getY();
        double dz = target.getZ() - this.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horizontal * 0.2, dz, 1.6F, 14 - this.level().getDifficulty().getId() * 4);
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level().addFreshEntity(arrow);
    }

    @Override
    public boolean canFireProjectileWeapon(ProjectileWeaponItem weapon) {
        return weapon instanceof BowItem;
    }

    @Override
    public String voice() {
        return "small";
    }

    @Override
    protected float pitchFactor() {
        return 1.25F;
    }
}
