package com.goblinraiders.entity;

import com.goblinraiders.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.block.StorehouseCrateBlockEntity;
import com.goblinraiders.network.DialogChoicePayload;
import com.goblinraiders.network.ModNetwork;
import com.goblinraiders.network.OpenQuestDialogPayload;
import com.goblinraiders.quest.Quests;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;

/**
 * The headman of a peaceful village: stands by the notice board on the square and hands out errands ({@link Quests}) -
 * goods for the storehouse, monsters to hunt, the sage to visit, a raiders' den to scout, a caravan to catch - paying in
 * goblin coins. Delivered goods go straight into the village storehouse. Never fights.
 */
public class GoblinHeadman extends AbstractGoblin {
    private static final int TALK_TICKS = 20 * 60;
    private static final int STORE_RANGE = 48;

    public GoblinHeadman(EntityType<? extends GoblinHeadman> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return goblinAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.23)
                .add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override
    protected void registerCombatGoals() {
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.3));
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.WRITABLE_BOOK));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        return data;
    }

    // ---------------------------------------------------------------- errands

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || player.getItemInHand(hand).is(Items.NAME_TAG) || !this.isAlive()) {
            return super.mobInteract(player, hand);
        }
        if (this.wakeIfDozing()) {
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            this.getNavigation().stop();
            this.startTalking(player, TALK_TICKS);
            this.openDialog(serverPlayer);
            this.playSound(this.getTalkSound(), this.getSoundVolume(), this.getVoicePitch());
            GoblinAdvancements.award(serverPlayer, GoblinAdvancements.SETTLER_TALK);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    private void openDialog(ServerPlayer player) {
        Quests.Quest quest = Quests.current(player);
        ModNetwork.send(player, new OpenQuestDialogPayload(this.getId(), quest == null ? "" : quest.id(), Quests.progress(player),
                Quests.completed(player)));
    }

    @Override
    public void onDialogChoice(ServerPlayer player, int choice) {
        switch (choice) {
            case DialogChoicePayload.QUEST_TAKE -> {
                Quests.take(player, this.random);
                this.sayRandom("give");
                this.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
            }
            case DialogChoicePayload.QUEST_TURN_IN -> {
                List<ItemStack> goods = Quests.turnIn(player);
                if (goods != null) {
                    goods.forEach(this::store);
                    this.sayRandom("thanks");
                    this.playSound(ModSounds.GOBLIN_YES.get(), 1.0F, this.getVoicePitch());
                    this.playSound(ModSounds.COINS.get(), 1.0F, 1.0F);
                    player.playNotifySound(SoundEvents.PLAYER_LEVELUP, SoundSource.NEUTRAL, 0.6F, 1.3F);
                    if (this.level() instanceof ServerLevel level) {
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getEyeY() + 0.3, this.getZ(), 10, 0.4, 0.4, 0.4, 0.0);
                    }
                }
            }
            case DialogChoicePayload.QUEST_DROP -> {
                Quests.drop(player);
                this.sayRandom("sigh");
            }
            default -> {
                super.onDialogChoice(player, choice);
                return;
            }
        }
        this.openDialog(player);   // fresh state for the window
    }

    /** Delivered goods go into the nearest storehouse crate with room (whatever does not fit is left at the headman's feet). */
    private void store(ItemStack stack) {
        ChunkPos chunk = new ChunkPos(this.blockPosition());
        int r = (STORE_RANGE >> 4) + 1;
        for (int dx = -r; dx <= r && !stack.isEmpty(); dx++) {
            for (int dz = -r; dz <= r && !stack.isEmpty(); dz++) {
                if (!this.level().hasChunk(chunk.x + dx, chunk.z + dz)) {
                    continue;
                }
                for (BlockEntity entity : this.level().getChunk(chunk.x + dx, chunk.z + dz).getBlockEntities().values()) {
                    if (entity instanceof StorehouseCrateBlockEntity crate && entity.getBlockPos().distSqr(this.blockPosition()) < STORE_RANGE * STORE_RANGE) {
                        stack = HopperBlockEntity.addItem(null, crate, stack, Direction.UP);
                        if (stack.isEmpty()) {
                            return;
                        }
                    }
                }
            }
        }
        if (!stack.isEmpty()) {
            this.spawnAtLocation(stack);
        }
    }

    // ---------------------------------------------------------------- never a fight

    @Override
    public void setTarget(@Nullable LivingEntity target) {
    }

    @Override
    public boolean wantsToAttack(Player player) {
        return false;
    }

    @Override
    protected boolean fightsBack() {
        return false;
    }

    @Override
    public boolean talksWith(Player player) {
        return true;
    }

    @Override
    public void raiseAlarm(Player intruder) {
    }

    @Override
    public boolean isFamiliar(Player player) {
        return true;
    }

    @Override
    protected int sleepersPercent() {
        return 100;
    }

    @Override
    protected boolean keepsWatch() {
        return false;
    }

    @Override
    protected int homeRadius() {
        return 10;
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
    public String voice() {
        return "headman";
    }

    @Override
    protected float pitchFactor() {
        return 0.9F;
    }

}
