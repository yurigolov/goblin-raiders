package com.goblinraiders.raid;

import com.goblinraiders.registry.ModSounds;
import com.goblinraiders.entity.work.WorkJob;
import com.goblinraiders.entity.GoblinChild;
import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.entity.AbstractGoblin;
import com.goblinraiders.registry.ModEffects;
import com.goblinraiders.registry.ModEntities;
import com.goblinraiders.registry.ModItems;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The goblin king's horde. A vassal pays tribute ({@link #TRIBUTE_COST} coins every three days - the king's collector
 * takes it from the inventory by himself), wears the goblin disguise as long as they are in good standing, is never
 * raided, and can hire goblins. Missing a tribute for a day gets them thrown out.
 */
@EventBusSubscriber(modid = GoblinRaiders.MODID)
public final class Horde {
    public static final int TRIBUTE_COST = 6;
    public static final long TRIBUTE_PERIOD = 72000;
    public static final long GRACE = 24000;
    private static final long DISGRACE_TICKS = 1200;
    public static final long HIRE_TICKS = 12000;

    /** What can be hired: small, ruthless, giant. */
    public enum Hireling {
        SMALL(3), RUTHLESS(6), GIANT(15);

        public final int cost;

        Hireling(int cost) {
            this.cost = cost;
        }

        EntityType<? extends AbstractGoblin> type() {
            return switch (this) {
                case SMALL -> ModEntities.SMALL_GOBLIN.get();
                case RUTHLESS -> ModEntities.RUTHLESS_GOBLIN.get();
                case GIANT -> ModEntities.GIANT_GOBLIN.get();
            };
        }
    }

    @Nullable
    public static GoblinRaidData.Vassal vassal(Player player) {
        return player.getServer() == null ? null : GoblinRaidData.get(player.getServer()).vassal(player.getUUID());
    }

    public static boolean isVassal(Player player) {
        return vassal(player) != null;
    }

    /** The player swears allegiance to the king. */
    public static void submit(ServerPlayer player) {
        if (isVassal(player) || isTraitor(player)) {
            return;
        }
        GoblinRaidData.get(player.server).addVassal(player.getUUID(), player.serverLevel().getGameTime() + TRIBUTE_PERIOD);
        if (!player.getInventory().add(new ItemStack(ModItems.VASSAL_CHARTER.get()))) {
            player.drop(new ItemStack(ModItems.VASSAL_CHARTER.get()), false);
        }
        player.sendSystemMessage(Component.translatable("goblinraiders.horde.joined", TRIBUTE_COST).withStyle(ChatFormatting.DARK_GREEN));
        GoblinAdvancements.award(player, GoblinAdvancements.VASSAL);
        refreshDisguise(player);
    }

    /** Pays the next tribute now. Returns whether anything was paid. */
    public static boolean payTribute(ServerPlayer player) {
        GoblinRaidData.Vassal vassal = vassal(player);
        if (vassal == null) {
            return false;
        }
        long now = player.serverLevel().getGameTime();
        if (vassal.nextTribute - now >= TRIBUTE_PERIOD) {
            player.displayClientMessage(Component.translatable("goblinraiders.horde.too_early"), true);
            return false;
        }
        if (!takeCoins(player, TRIBUTE_COST)) {
            player.displayClientMessage(Component.translatable("goblinraiders.horde.not_enough", TRIBUTE_COST).withStyle(ChatFormatting.RED), true);
            return false;
        }
        vassal.nextTribute = Math.max(vassal.nextTribute, now) + TRIBUTE_PERIOD;
        vassal.warned = false;
        GoblinRaidData.get(player.server).setDirty();
        player.displayClientMessage(Component.translatable("goblinraiders.horde.paid").withStyle(ChatFormatting.GOLD), true);
        player.playNotifySound(ModSounds.KING_AMBIENT.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        return true;
    }

    /** Hires a goblin for {@link #HIRE_TICKS}. */
    public static boolean hire(ServerPlayer player, Hireling kind) {
        if (!isVassal(player)) {
            return false;
        }
        if (!takeCoins(player, kind.cost)) {
            player.displayClientMessage(Component.translatable("goblinraiders.horde.not_enough", kind.cost).withStyle(ChatFormatting.RED), true);
            return false;
        }
        AbstractGoblin goblin = spawnMercenary(player, kind.type(), HIRE_TICKS);
        if (goblin != null) {
            player.sendSystemMessage(Component.translatable("goblinraiders.horde.hired", goblin.getDisplayName()).withStyle(ChatFormatting.DARK_GREEN));
            GoblinAdvancements.award(player, GoblinAdvancements.HIRE);
        }
        return goblin != null;
    }

    /** A goblin child for the player's farm or mine (it follows them until told where to work). */
    public static final int WORKER_COST = 8;

    public static boolean buyWorker(ServerPlayer player, WorkJob job) {
        if (!isVassal(player)) {
            return false;
        }
        if (!takeCoins(player, WORKER_COST)) {
            player.displayClientMessage(Component.translatable("goblinraiders.horde.not_enough", WORKER_COST).withStyle(ChatFormatting.RED), true);
            return false;
        }
        ServerLevel level = player.serverLevel();
        GoblinChild child = ModEntities.GOBLIN_CHILD.get().create(level);
        if (child == null) {
            return false;
        }
        double a = level.random.nextDouble() * Math.PI * 2;
        child.moveTo(player.getX() + Math.cos(a) * 1.5, player.getY(), player.getZ() + Math.sin(a) * 1.5, player.getYRot(), 0.0F);
        child.finalizeSpawn(level, level.getCurrentDifficultyAt(child.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        child.employ(player, job);
        level.addFreshEntity(child);
        level.sendParticles(ParticleTypes.POOF, child.getX(), child.getY() + 0.5, child.getZ(), 10, 0.2, 0.3, 0.2, 0.02);
        player.sendSystemMessage(Component.translatable("goblinraiders.horde.worker_" + (job == WorkJob.MINER ? "miner" : "farmer"))
                .withStyle(ChatFormatting.DARK_GREEN));
        GoblinAdvancements.award(player, GoblinAdvancements.ARTEL);
        return true;
    }

    /** Spawns a goblin next to the player that follows and protects them for {@code ticks}. */
    @Nullable
    public static AbstractGoblin spawnMercenary(ServerPlayer player, EntityType<? extends AbstractGoblin> type, long ticks) {
        ServerLevel level = player.serverLevel();
        AbstractGoblin goblin = type.create(level);
        if (goblin == null) {
            return null;
        }
        double a = level.random.nextDouble() * Math.PI * 2;
        goblin.moveTo(player.getX() + Math.cos(a) * 1.5, player.getY(), player.getZ() + Math.sin(a) * 1.5, player.getYRot(), 0.0F);
        goblin.finalizeSpawn(level, level.getCurrentDifficultyAt(goblin.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        goblin.hireBy(player.getUUID(), level.getGameTime() + ticks);
        level.addFreshEntity(goblin);
        level.sendParticles(ParticleTypes.POOF, goblin.getX(), goblin.getY() + 1.0, goblin.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
        return goblin;
    }

    /** A mercenary's time is up. */
    public static void dismiss(AbstractGoblin goblin) {
        if (goblin.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.POOF, goblin.getX(), goblin.getY() + 1.0, goblin.getZ(), 12, 0.3, 0.5, 0.3, 0.02);
            Player hirer = goblin.getHirer();
            if (hirer != null) {
                hirer.displayClientMessage(Component.translatable("goblinraiders.horde.dismissed", goblin.getDisplayName()), true);
            }
        }
        goblin.discard();
    }

    /** A vassal struck a goblin: the horde stops covering for them for a minute. */
    public static void disgrace(Player player) {
        GoblinRaidData.Vassal vassal = vassal(player);
        if (vassal != null) {
            vassal.disgracedUntil = player.level().getGameTime() + DISGRACE_TICKS;
        }
    }

    public static boolean isTraitor(Player player) {
        return player.getServer() != null && GoblinRaidData.get(player.getServer()).isTraitor(player.getUUID());
    }

    /** The vassal turns on the king: out of the horde for good, no disguise, the charter crumbles. Raids resume. */
    public static void betray(ServerPlayer player) {
        GoblinRaidData data = GoblinRaidData.get(player.server);
        data.removeVassal(player.getUUID());
        data.markTraitor(player.getUUID());
        player.removeEffect(ModEffects.DISGUISE);
        player.getInventory().clearOrCountMatchingItems(stack -> stack.is(ModItems.VASSAL_CHARTER.get()), -1, player.inventoryMenu.getCraftSlots());
        player.sendSystemMessage(Component.translatable("goblinraiders.horde.betrayed").withStyle(ChatFormatting.RED));
        GoblinAdvancements.award(player, GoblinAdvancements.BETRAY);
    }

    public static void expel(ServerPlayer player, String messageKey) {
        GoblinRaidData.get(player.server).removeVassal(player.getUUID());
        player.removeEffect(ModEffects.DISGUISE);
        player.sendSystemMessage(Component.translatable(messageKey).withStyle(ChatFormatting.RED));
    }

    public static int coinsOf(Player player) {
        int coins = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.GOBLIN_COIN.get())) {
                coins += stack.getCount();
            }
        }
        return coins;
    }

    private static boolean takeCoins(Player player, int amount) {
        if (coinsOf(player) < amount) {
            return false;
        }
        int left = amount;
        for (ItemStack stack : player.getInventory().items) {
            if (left > 0 && stack.is(ModItems.GOBLIN_COIN.get())) {
                int take = Math.min(left, stack.getCount());
                stack.shrink(take);
                left -= take;
            }
        }
        return true;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 100 != 0) {
            return;
        }
        for (ServerPlayer player : java.util.List.copyOf(event.getServer().getPlayerList().getPlayers())) {
            tickVassal(player);
        }
    }

    /** Tribute collection and the disguise of one vassal. */
    public static void tickVassal(ServerPlayer player) {
        GoblinRaidData.Vassal vassal = vassal(player);
        if (vassal == null) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        if (now >= vassal.nextTribute) {
            if (takeCoins(player, TRIBUTE_COST)) {
                vassal.nextTribute += TRIBUTE_PERIOD;
                vassal.warned = false;
                GoblinRaidData.get(player.server).setDirty();
                player.sendSystemMessage(Component.translatable("goblinraiders.horde.collected", TRIBUTE_COST).withStyle(ChatFormatting.GOLD));
            } else if (now >= vassal.nextTribute + GRACE) {
                expel(player, "goblinraiders.horde.expelled");
                return;
            } else if (!vassal.warned) {
                vassal.warned = true;
                GoblinRaidData.get(player.server).setDirty();
                player.sendSystemMessage(Component.translatable("goblinraiders.horde.overdue", TRIBUTE_COST).withStyle(ChatFormatting.RED));
            }
        }
        if (now >= vassal.disgracedUntil) {
            refreshDisguise(player);
        }
    }

    private static void refreshDisguise(ServerPlayer player) {
        MobEffectInstance current = player.getEffect(ModEffects.DISGUISE);
        if (current == null || current.getDuration() < 200) {
            player.addEffect(new MobEffectInstance(ModEffects.DISGUISE, 600, 0, true, false, true));
        }
    }

    private Horde() {
    }
}
