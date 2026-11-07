package com.goblinraiders.entity;

import com.goblinraiders.registry.ModSounds;
import com.goblinraiders.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Skin 2: player-sized, hits hard and always carries a goblin sword. */
public class RuthlessGoblin extends AbstractGoblin {
    private static final float ARMOR_CHANCE = 0.2F;

    public RuthlessGoblin(EntityType<? extends RuthlessGoblin> type, Level level) {
        super(type, level);
        this.xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return goblinAttributes()
                .add(Attributes.MAX_HEALTH, 26.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 4.0)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void registerCombatGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        this.addHostileTargets();
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.GOBLIN_SWORD.get()));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.1F);
        float chance = ARMOR_CHANCE * (0.5F + difficulty.getSpecialMultiplier());
        equipArmor(random, chance, EquipmentSlot.HEAD, ModItems.GOBLIN_HELMET.get());
        equipArmor(random, chance, EquipmentSlot.CHEST, ModItems.GOBLIN_CHESTPLATE.get());
        equipArmor(random, chance, EquipmentSlot.LEGS, ModItems.GOBLIN_LEGGINGS.get());
        equipArmor(random, chance, EquipmentSlot.FEET, ModItems.GOBLIN_BOOTS.get());
    }

    private void equipArmor(RandomSource random, float chance, EquipmentSlot slot, Item item) {
        if (random.nextFloat() < chance) {
            this.setItemSlot(slot, new ItemStack(item));
            this.setDropChance(slot, 0.05F);
        }
    }

    @Override
    public String voice() {
        return "ruthless";
    }

    @Override
    protected float pitchFactor() {
        return 0.9F;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.random.nextBoolean() ? ModSounds.RUTHLESS_AMBIENT.get() : super.getAmbientSound();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return ModSounds.RUTHLESS_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.RUTHLESS_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState block) {
        this.playSound(SoundEvents.PIGLIN_BRUTE_STEP, 0.15F, 1.0F);
    }
}
