package com.goblinraiders.raid;

import com.goblinraiders.quest.Quests;
import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.entity.AbstractGoblin;
import com.goblinraiders.registry.ModEntities;
import com.goblinraiders.registry.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Raid caravans: after a raid the loot leaves the base with goblins heading for their village. On paper they move
 * {@link #SPEED} blocks a second; as soon as a player comes within {@link #WAKE_RADIUS} they become real goblins -
 * small carriers with sacks (who run from players) and a guard or two. Kill a carrier and its share of the loot drops.
 * Whatever reaches the village ends up in its stash, just like before.
 */
@EventBusSubscriber(modid = GoblinRaiders.MODID)
public final class Caravans {
    public static final double SPEED = 2.5;
    public static final double ARRIVE_RADIUS = 24.0;
    public static final double WAKE_RADIUS = 64.0;
    private static final double SLEEP_RADIUS = 96.0;
    private static final long SLEEP_TICKS = 200;
    /** After this long the caravan is home no matter what (20 minutes). */
    private static final long MAX_AGE = 24000;

    /** Starts a caravan at {@code from} carrying {@code loot} to the village whose center is {@code village}. */
    public static Caravan start(ServerLevel level, UUID owner, BlockPos from, BlockPos village, List<ItemStack> loot) {
        int carriers = Mth.clamp((loot.size() + 8) / 9, 1, 4);
        List<List<ItemStack>> shares = new ArrayList<>();
        for (int i = 0; i < carriers; i++) {
            shares.add(new ArrayList<>());
        }
        for (int i = 0; i < loot.size(); i++) {
            shares.get(i % carriers).add(loot.get(i));
        }
        Caravan caravan = new Caravan(UUID.randomUUID(), owner, new BlockPos(village.getX(), 0, village.getZ()), level.getGameTime(),
                from.getX() + 0.5, from.getZ() + 0.5, shares);
        GoblinRaidData.get(level.getServer()).addCaravan(caravan);
        return caravan;
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 == 0) {
            tick(event.getServer().overworld());
        }
    }

    /** One second of caravan life. */
    public static void tick(ServerLevel level) {
        GoblinRaidData data = GoblinRaidData.get(level.getServer());
        long now = level.getGameTime();
        for (Caravan caravan : List.copyOf(data.caravans())) {
            if (now - caravan.startTime > MAX_AGE) {
                arrive(level, data, caravan);
            } else if (caravan.materialized) {
                tickMaterialized(level, data, caravan, now);
            } else {
                tickOnPaper(level, data, caravan, now);
            }
        }
    }

    private static void tickOnPaper(ServerLevel level, GoblinRaidData data, Caravan caravan, long now) {
        double dist = caravan.distanceToVillage();
        if (dist <= ARRIVE_RADIUS) {
            arrive(level, data, caravan);
            return;
        }
        double step = Math.min(SPEED, dist);
        caravan.x += (caravan.village.getX() + 0.5 - caravan.x) / dist * step;
        caravan.z += (caravan.village.getZ() + 0.5 - caravan.z) / dist * step;
        data.setDirty();
        BlockPos here = BlockPos.containing(caravan.x, 64, caravan.z);
        if (playerNear(level, caravan.x, caravan.z, WAKE_RADIUS) && level.hasChunkAt(here)) {
            materialize(level, caravan, now);
        }
    }

    /** Turns the caravan into goblins at its current position. */
    public static void materialize(ServerLevel level, Caravan caravan, long now) {
        int x = Mth.floor(caravan.x), z = Mth.floor(caravan.z);
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        caravan.members.clear();
        for (int i = 0; i < caravan.shares.size(); i++) {
            if (!caravan.shares.get(i).isEmpty()) {
                spawn(level, ModEntities.SMALL_GOBLIN.get(), caravan, i, x + (i % 2) * 1.5, y, z + (i / 2) * 1.5);
            }
        }
        int guards = caravan.shares.size() >= 3 ? 2 : 1;
        for (int g = 0; g < guards; g++) {
            spawn(level, ModEntities.RUTHLESS_GOBLIN.get(), caravan, -1, x - 1.5 - g, y, z - 1.0);
        }
        caravan.materialized = true;
        caravan.lastWatched = now;
        GoblinRaidData.get(level.getServer()).setDirty();
    }

    private static void spawn(ServerLevel level, EntityType<? extends AbstractGoblin> type, Caravan caravan, int share,
                              double x, int y, double z) {
        AbstractGoblin goblin = type.create(level);
        if (goblin == null) {
            return;
        }
        goblin.moveTo(x + 0.5, y, z + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
        goblin.finalizeSpawn(level, level.getCurrentDifficultyAt(goblin.blockPosition()), MobSpawnType.EVENT, null);
        if (share >= 0) {
            goblin.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.LOOT_SACK.get()));
            goblin.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        }
        goblin.joinCaravan(caravan.id, share);
        level.addFreshEntity(goblin);
        caravan.members.add(goblin.getUUID());
    }

    private static void tickMaterialized(ServerLevel level, GoblinRaidData data, Caravan caravan, long now) {
        List<AbstractGoblin> alive = members(level, caravan);
        if (!caravan.hasLoot()) {
            // Every carrier is down: the guards forget about the caravan and stay where they are.
            alive.forEach(g -> g.joinCaravan(null, -1));
            data.removeCaravan(caravan.id);
            return;
        }
        if (alive.isEmpty()) {
            // Unloaded (or the server restarted): back on paper from the last known position.
            caravan.materialized = false;
            caravan.members.clear();
            data.setDirty();
            return;
        }
        AbstractGoblin leader = alive.stream().filter(AbstractGoblin::isCarryingLoot).findFirst().orElse(alive.get(0));
        caravan.x = leader.getX();
        caravan.z = leader.getZ();
        data.setDirty();
        if (caravan.distanceToVillage() <= ARRIVE_RADIUS) {
            arrive(level, data, caravan);
            return;
        }
        if (playerNear(level, caravan.x, caravan.z, SLEEP_RADIUS)) {
            caravan.lastWatched = now;
        } else if (now - caravan.lastWatched > SLEEP_TICKS) {
            alive.forEach(AbstractGoblin::discard);
            caravan.materialized = false;
            caravan.members.clear();
        }
    }

    /** The loot reaches the village stash. */
    private static void arrive(ServerLevel level, GoblinRaidData data, Caravan caravan) {
        List<ItemStack> loot = caravan.allLoot();
        if (!loot.isEmpty()) {
            data.addStolen(new ChunkPos(caravan.village), loot);
            Player owner = level.getServer().getPlayerList().getPlayer(caravan.owner);
            if (owner != null) {
                owner.sendSystemMessage(Component.translatable("goblinraiders.caravan.arrived").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            }
        }
        members(level, caravan).forEach(AbstractGoblin::discard);
        data.removeCaravan(caravan.id);
    }

    /** A loot carrier died: its share drops on the ground. */
    public static void onCarrierDeath(AbstractGoblin goblin, DamageSource source) {
        ServerLevel level = (ServerLevel) goblin.level();
        GoblinRaidData data = GoblinRaidData.get(level.getServer());
        Caravan caravan = data.caravan(goblin.getCaravanId());
        int share = goblin.getCaravanShare();
        if (caravan == null || share < 0 || share >= caravan.shares.size()) {
            return;
        }
        for (ItemStack stack : caravan.shares.get(share)) {
            goblin.spawnAtLocation(stack.copy());
        }
        caravan.shares.set(share, new ArrayList<>());
        data.setDirty();
        if (source.getEntity() instanceof Player player) {
            String key = caravan.hasLoot() ? "goblinraiders.caravan.share" : "goblinraiders.caravan.all";
            player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD), true);
            GoblinAdvancements.award(player, GoblinAdvancements.INTERCEPT);
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                Quests.onCaravanCarrier(serverPlayer);
            }
            if (!caravan.hasLoot()) {
                GoblinAdvancements.award(player, GoblinAdvancements.WHOLE_CARAVAN);
            }
        }
    }

    /** Where a caravan goblin is heading, or null if it does not belong to a caravan any more. */
    @Nullable
    public static BlockPos destinationOf(AbstractGoblin goblin) {
        if (goblin.getCaravanId() == null || goblin.level().getServer() == null) {
            return null;
        }
        Caravan caravan = GoblinRaidData.get(goblin.level().getServer()).caravan(goblin.getCaravanId());
        return caravan == null ? null : caravan.village;
    }

    private static List<AbstractGoblin> members(ServerLevel level, Caravan caravan) {
        List<AbstractGoblin> alive = new ArrayList<>();
        for (UUID id : caravan.members) {
            if (level.getEntity(id) instanceof AbstractGoblin goblin && goblin.isAlive()) {
                alive.add(goblin);
            }
        }
        return alive;
    }

    private static boolean playerNear(ServerLevel level, double x, double z, double radius) {
        for (Player player : level.players()) {
            if (!player.isSpectator()) {
                double dx = player.getX() - x, dz = player.getZ() - z;
                if (dx * dx + dz * dz <= radius * radius) {
                    return true;
                }
            }
        }
        return false;
    }

    private Caravans() {
    }
}
