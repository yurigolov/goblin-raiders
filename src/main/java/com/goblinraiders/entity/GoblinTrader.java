package com.goblinraiders.entity;

import com.goblinraiders.network.ModNetwork;
import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.network.DialogChoicePayload;
import com.goblinraiders.network.OpenDialogPayload;
import com.goblinraiders.registry.ModItems;
import com.goblinraiders.registry.ModSounds;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Skin 5: the goblin trader. Clicking him opens a dialog with his prices; agreeing opens the usual merchant window
 * (prices in goblin coins), refusing makes him yell and attack. Stock refills every Minecraft day.
 */
public class GoblinTrader extends AbstractGoblin implements Merchant {
    private static final int DIALOG_TICKS = 20 * 60;
    private static final int ANGER_TICKS = 20 * 40;
    private static final long RESTOCK_INTERVAL = 24000L;

    @Nullable
    private Player tradingPlayer;
    @Nullable
    private MerchantOffers offers;
    private long lastRestock = -1;
    private int angerTicks;
    @Nullable
    private UUID angryAt;

    public GoblinTrader(EntityType<? extends GoblinTrader> type, Level level) {
        super(type, level);
        this.xpReward = 6;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return goblinAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.27)
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void registerCombatGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2, true));
    }

    // ---------------------------------------------------------------- dialog

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive() || player.getItemInHand(hand).is(Items.NAME_TAG) || !this.isAlive()) {
            return super.mobInteract(player, hand);
        }
        if (this.wakeIfDozing()) {
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            if (this.isAngryAt(player)) {
                this.sayRandom(Speech.GO_AWAY);
            } else if (this.tradingPlayer != null && this.tradingPlayer != player) {
                this.playSound(ModSounds.GOBLIN_NO.get(), this.getSoundVolume(), this.getVoicePitch());
            } else {
                this.restockIfNeeded();
                this.startTalking(player, DIALOG_TICKS);
                ModNetwork.send(serverPlayer, new OpenDialogPayload(this.getId(), this.getOffers()));
                this.playSound(ModSounds.TRADER_HAGGLE.get(), this.getSoundVolume(), this.getVoicePitch());
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    /** The player's answer from the dialog window. */
    @Override
    public void onDialogChoice(ServerPlayer player, int choice) {
        if (this.isAngryAt(player)) {
            return;
        }
        switch (choice) {
            case DialogChoicePayload.ACCEPT -> {
                if (this.tradingPlayer == null || this.tradingPlayer == player) {
                    this.setTradingPlayer(player);
                    this.openTradingScreen(player, this.getDisplayName(), 1);
                }
            }
            case DialogChoicePayload.REFUSE -> {
                this.getAngryAt(player);
                GoblinAdvancements.award(player, GoblinAdvancements.REFUSE);
            }
            default -> {
                if (this.getTalkingTo() == player && this.tradingPlayer == null) {
                    this.stopTalking();
                }
            }
        }
    }

    /** "What?! You won't trade with me?!" - yells for everyone around to hear and attacks. */
    public void getAngryAt(Player player) {
        this.stopTrading();
        this.stopTalking();
        GoblinFriends.betray(player);
        this.angryAt = player.getUUID();
        this.angerTicks = ANGER_TICKS;
        this.shoutRandom(Speech.YELL, this.playersAround(24.0));
        this.playSound(ModSounds.TRADER_YELL.get(), 2.0F, this.getVoicePitch());
        if (this.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getEyeY() + 0.3, this.getZ(), 6, 0.4, 0.3, 0.4, 0.0);
        }
        this.setTarget(player);
    }

    public boolean isAngryAt(Player player) {
        return this.getTarget() == player || this.angerTicks > 0 && player.getUUID().equals(this.angryAt);
    }

    @Override
    public boolean holdsGrudge(Player player) {
        return this.angerTicks > 0 && player.getUUID().equals(this.angryAt) || super.holdsGrudge(player);
    }

    /** The shop closes at night: the trader always sleeps. */
    @Override
    protected int sleepersPercent() {
        return 100;
    }

    public boolean isAngry() {
        return this.angerTicks > 0;
    }

    @Override
    protected void onStartAttacking(Player player) {
        // getAngryAt already yelled; a trader attacked first still shouts back.
        if (this.angerTicks == 0) {
            this.angryAt = player.getUUID();
            this.angerTicks = ANGER_TICKS;
            super.onStartAttacking(player);
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.angerTicks > 0) {
            this.angerTicks--;
            if (this.getTarget() instanceof Player player && this.random.nextInt(120) == 0) {
                this.shoutRandom(Speech.YELL, this.playersAround(24.0));
                this.playSound(ModSounds.TRADER_YELL.get(), 1.5F, this.getVoicePitch());
            }
            if (this.angerTicks == 0) {
                this.setTarget(null);
                this.angryAt = null;
                this.sayRandom(Speech.CALM);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide) {
            this.stopTrading();
        }
        return hurt;
    }

    @Override
    public void die(DamageSource damageSource) {
        super.die(damageSource);
        this.stopTrading();
    }

    private void stopTrading() {
        if (this.tradingPlayer != null) {
            Player player = this.tradingPlayer;
            this.setTradingPlayer(null);
            player.closeContainer();
        }
    }

    // ---------------------------------------------------------------- trades

    /** The goblin price list: what he sells for coins, and what he pays coins for. */
    public static MerchantOffers createOffers() {
        MerchantOffers offers = new MerchantOffers();
        Item coin = ModItems.GOBLIN_COIN.get();
        offers.add(sell(coin, 24, ModItems.GOBLIN_SWORD.get(), 1, 3));
        offers.add(sell(coin, 14, ModItems.GOBLIN_HELMET.get(), 1, 2));
        offers.add(sell(coin, 24, ModItems.GOBLIN_CHESTPLATE.get(), 1, 2));
        offers.add(sell(coin, 20, ModItems.GOBLIN_LEGGINGS.get(), 1, 2));
        offers.add(sell(coin, 12, ModItems.GOBLIN_BOOTS.get(), 1, 2));
        offers.add(new MerchantOffer(new ItemCost(coin, 16), Optional.of(new ItemCost(Items.ENDER_PEARL, 1)),
                new ItemStack(ModItems.TELEPORT_AMULET.get()), 3, 0, 0.0F));
        offers.add(sell(coin, 18, ModItems.CONCEALMENT_AMULET.get(), 1, 2));
        offers.add(sell(coin, 1, ModItems.GOBLIN_GRUB.get(), 2, 12));
        offers.add(sell(coin, 6, Items.GOLDEN_APPLE, 1, 5));
        offers.add(sell(coin, 4, Items.ENDER_PEARL, 1, 8));
        offers.add(sell(coin, 3, Items.SADDLE, 1, 2));
        offers.add(sell(Items.EMERALD, 1, coin, 2, 16));
        offers.add(sell(Items.GOLD_INGOT, 3, coin, 2, 12));
        offers.add(sell(Items.DIAMOND, 1, coin, 5, 8));
        offers.add(sell(Items.ROTTEN_FLESH, 16, coin, 1, 16));
        return offers;
    }

    private static MerchantOffer sell(ItemLike cost, int costCount, ItemLike result, int resultCount, int maxUses) {
        return new MerchantOffer(new ItemCost(cost, costCount), new ItemStack(result, resultCount), maxUses, 0, 0.0F);
    }

    private void restockIfNeeded() {
        long now = this.level().getGameTime();
        if (this.lastRestock < 0 || now - this.lastRestock >= RESTOCK_INTERVAL) {
            this.getOffers().forEach(MerchantOffer::resetUses);
            this.lastRestock = now;
        }
    }

    @Override
    public void setTradingPlayer(@Nullable Player player) {
        this.tradingPlayer = player;
        if (player != null) {
            this.startTalking(player, Integer.MAX_VALUE);
        } else {
            this.stopTalking();
        }
    }

    @Nullable
    @Override
    public Player getTradingPlayer() {
        return this.tradingPlayer;
    }

    @Override
    public MerchantOffers getOffers() {
        if (this.offers == null) {
            this.offers = createOffers();
        }
        return this.offers;
    }

    @Override
    public void overrideOffers(MerchantOffers offers) {
        // Only used client side by the vanilla client merchant.
    }

    @Override
    public void notifyTrade(MerchantOffer offer) {
        this.playSound(ModSounds.COINS.get(), 1.0F, 1.0F);
        offer.increaseUses();
        if (this.tradingPlayer != null) {
            GoblinAdvancements.award(this.tradingPlayer, GoblinAdvancements.TRADE);
        }
        this.ambientSoundTime = -this.getAmbientSoundInterval();
        if (this.random.nextInt(3) == 0) {
            this.sayRandom(Speech.THANKS);
        }
    }

    @Override
    public void notifyTradeUpdated(ItemStack stack) {
        if (!this.level().isClientSide && this.ambientSoundTime > -this.getAmbientSoundInterval() + 20) {
            this.ambientSoundTime = -this.getAmbientSoundInterval();
            this.playSound(stack.isEmpty() ? ModSounds.GOBLIN_NO.get() : ModSounds.TRADER_HAGGLE.get(), this.getSoundVolume(), this.getVoicePitch());
        }
    }

    @Override
    public int getVillagerXp() {
        return 0;
    }

    @Override
    public void overrideXp(int xp) {
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return ModSounds.TRADER_HAGGLE.get();
    }

    @Override
    public boolean canRestock() {
        return true;
    }

    @Override
    public boolean isClientSide() {
        return this.level().isClientSide;
    }

    // ---------------------------------------------------------------- saving

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.offers != null) {
            MerchantOffers.CODEC.encodeStart(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), this.offers)
                    .result().ifPresent(offersTag -> tag.put("Offers", offersTag));
        }
        tag.putLong("LastRestock", this.lastRestock);
        tag.putInt("AngerTicks", this.angerTicks);
        if (this.angryAt != null) {
            tag.putUUID("AngryAt", this.angryAt);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Offers")) {
            MerchantOffers.CODEC.parse(this.registryAccess().createSerializationContext(NbtOps.INSTANCE), tag.get("Offers"))
                    .result().ifPresent(parsed -> this.offers = parsed);
        }
        this.lastRestock = tag.contains("LastRestock") ? tag.getLong("LastRestock") : -1;
        this.angerTicks = tag.getInt("AngerTicks");
        this.angryAt = tag.hasUUID("AngryAt") ? tag.getUUID("AngryAt") : null;
    }

    // ---------------------------------------------------------------- misc

    @Override
    public boolean canChat() {
        return super.canChat() && this.tradingPlayer == null && this.angerTicks == 0;
    }

    @Override
    public boolean isPreventingPlayerRest(Player player) {
        return this.isAngryAt(player);
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    /** The trader keeps close to his shop. */
    @Override
    protected int homeRadius() {
        return 8;
    }

    @Override
    public String voice() {
        return "trader";
    }

    @Override
    protected float pitchFactor() {
        return 1.2F;
    }

    @Override
    protected SoundEvent getTalkSound() {
        return ModSounds.TRADER_HAGGLE.get();
    }

    @Override
    protected SoundEvent getAngrySound() {
        return ModSounds.TRADER_YELL.get();
    }
}
