package com.goblinraiders.entity;

import com.goblinraiders.registry.ModSounds;
import com.goblinraiders.quest.Quests;
import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.network.DialogChoicePayload;
import com.goblinraiders.network.ModNetwork;
import com.goblinraiders.network.OpenDialogPayload;
import com.goblinraiders.raid.GoblinRaids;
import com.goblinraiders.raid.Horde;
import com.goblinraiders.registry.ModTags;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * The goblin sage: a hermit in a lone tent somewhere out in the wild. He shares wisdom, foretells where the nearest
 * peaceful village, goblin den and the king's city lie (once a day for each player), and blesses travellers for a few
 * coins. He never fights: whoever strikes him finds only a puff of smoke where he stood.
 */
public class GoblinSage extends AbstractGoblin {
    public static final int BLESSING_COST = 3;
    /** How far (in chunks) the sage's visions reach. */
    private static final int VISION_CHUNKS = 64;
    private static final String LAST_PROPHECY_KEY = "goblinraiders.last_prophecy";

    public GoblinSage(EntityType<? extends GoblinSage> type, Level level) {
        super(type, level);
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return goblinAttributes()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.MOVEMENT_SPEED, 0.2)
                .add(Attributes.ATTACK_DAMAGE, 1.0);
    }

    @Override
    protected void registerCombatGoals() {
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType spawnType,
                                        @Nullable SpawnGroupData spawnGroupData) {
        SpawnGroupData data = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOOK));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        if (this.getHome() == null) {
            this.setHome(this.blockPosition());
        }
        return data;
    }

    // ---------------------------------------------------------------- talking

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || player.getItemInHand(hand).is(Items.NAME_TAG) || !this.isAlive()) {
            return super.mobInteract(player, hand);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            this.getNavigation().stop();
            this.startTalking(player, 20 * 60);
            ModNetwork.send(serverPlayer, new OpenDialogPayload(this.getId(), new MerchantOffers()));
            this.playSound(ModSounds.ELDER_AMBIENT.get(), 0.6F, 1.3F);
            GoblinAdvancements.award(serverPlayer, GoblinAdvancements.SAGE);
            Quests.onSageVisit(serverPlayer);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public void onDialogChoice(ServerPlayer player, int choice) {
        switch (choice) {
            case DialogChoicePayload.SAGE_PROPHECY -> this.prophesy(player);
            case DialogChoicePayload.SAGE_BLESSING -> this.bless(player);
            default -> super.onDialogChoice(player, choice);
        }
    }

    /** Where the nearest settlements lie: once a day for each player (the visions tire the old goblin). */
    public boolean prophesy(ServerPlayer player) {
        long day = this.level().getDayTime() / 24000L;
        if (player.getPersistentData().contains(LAST_PROPHECY_KEY) && player.getPersistentData().getLong(LAST_PROPHECY_KEY) == day) {
            this.tell(player, Component.translatable("goblinraiders.sage.tired"));
            return false;
        }
        player.getPersistentData().putLong(LAST_PROPHECY_KEY, day);
        this.tell(player, Component.translatable("goblinraiders.sage.vision"));
        for (String place : List.of("hamlet", "den", "capital")) {
            this.tell(player, this.vision(place));
        }
        this.cast(ParticleTypes.ENCHANT, 40);
        this.playSound(ModSounds.SHAMAN_SPELL.get(), 1.0F, 1.2F);
        GoblinAdvancements.award(player, GoblinAdvancements.PROPHECY);
        return true;
    }

    private Component vision(String place) {
        TagKey<Structure> tag = switch (place) {
            case "hamlet" -> ModTags.GOBLIN_HAMLETS;
            case "capital" -> ModTags.GOBLIN_CAPITALS;
            default -> ModTags.GOBLIN_VILLAGES;
        };
        BlockPos found = this.level() instanceof ServerLevel level
                ? level.findNearestMapStructure(tag, this.blockPosition(), VISION_CHUNKS, false) : null;
        if (found == null) {
            return Component.translatable("goblinraiders.sage.vision." + place + ".none");
        }
        int distance = (int) Math.sqrt(this.blockPosition().distSqr(new BlockPos(found.getX(), this.getBlockY(), found.getZ())));
        Component direction = Component.translatable("goblinraiders.direction." + GoblinRaids.compass(this.blockPosition(), found));
        return Component.translatable("goblinraiders.sage.vision." + place, direction, distance);
    }

    /** Luck and a little healing for a few goblin coins. */
    public boolean bless(ServerPlayer player) {
        if (Horde.coinsOf(player) < BLESSING_COST) {
            this.tell(player, Component.translatable("goblinraiders.sage.no_coins", BLESSING_COST));
            return false;
        }
        int left = BLESSING_COST;
        for (ItemStack stack : player.getInventory().items) {
            if (left > 0 && stack.is(com.goblinraiders.registry.ModItems.GOBLIN_COIN.get())) {
                int take = Math.min(left, stack.getCount());
                stack.shrink(take);
                left -= take;
            }
        }
        player.addEffect(new MobEffectInstance(MobEffects.LUCK, 20 * 60 * 5, 0), this);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 20 * 30, 0), this);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 20 * 60 * 5, 0), this);
        this.tell(player, Component.translatable("goblinraiders.sage.blessed"));
        this.cast(ParticleTypes.HAPPY_VILLAGER, 20);
        this.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.0F);
        GoblinAdvancements.award(player, GoblinAdvancements.BLESSING);
        return true;
    }

    private void tell(ServerPlayer player, Component line) {
        player.sendSystemMessage(Component.translatable("goblinraiders.speech.format", this.getDisplayName(), line)
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    private void cast(net.minecraft.core.particles.ParticleOptions particle, int count) {
        this.swing(InteractionHand.MAIN_HAND);
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(particle, this.getX(), this.getY() + 1.2, this.getZ(), count, 0.5, 0.6, 0.5, 0.2);
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

    /** Struck, the sage vanishes in smoke and turns up a few steps away. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.isAlive() && source.getEntity() instanceof LivingEntity) {
            this.stopTalking();
            this.sayRandom(Speech.HELP);
            if (this.level() instanceof ServerLevel level) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getY() + 1.0, this.getZ(), 20, 0.3, 0.6, 0.3, 0.02);
            }
            for (int i = 0; i < 16; i++) {
                double x = this.getX() + (this.random.nextDouble() - 0.5) * 16.0;
                double z = this.getZ() + (this.random.nextDouble() - 0.5) * 16.0;
                if (this.randomTeleport(x, this.getY() + this.random.nextInt(5) - 2, z, false)) {
                    this.playSound(SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.0F);
                    break;
                }
            }
        }
        return hurt;
    }

    /** Sages meditate through the night. */
    @Override
    protected int sleepersPercent() {
        return 0;
    }

    @Override
    protected boolean keepsWatch() {
        return false;
    }

    @Override
    protected boolean tendsFires() {
        return false;
    }

    @Override
    protected int homeRadius() {
        return 6;
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
    public boolean canChat() {
        return false;
    }

    @Override
    public String voice() {
        return "sage";
    }

    @Override
    protected float pitchFactor() {
        return 0.8F;
    }

    /** For game tests: the three places of a prophecy. */
    public static List<String> places() {
        return new ArrayList<>(List.of("hamlet", "den", "capital"));
    }
}
