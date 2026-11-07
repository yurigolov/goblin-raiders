package com.goblinraiders.raid;

import net.minecraft.nbt.NbtUtils;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * World-wide raid state, saved in the overworld's data folder (goblinraiders_raids.dat):
 * which chests make up each player's base, when they were last raided, the loot every village is holding,
 * caravans on their way home, and the players who swore allegiance to the goblin king.
 */
public class GoblinRaidData extends SavedData {
    private static final String NAME = "goblinraiders_raids";
    private static final int MAX_TRACKED_CHESTS = 512;

    private final Map<UUID, PlayerBase> bases = new HashMap<>();
    /** Stolen items per goblin village, keyed by the village's structure start chunk. */
    private final Map<Long, List<ItemStack>> stolen = new HashMap<>();
    private final Map<UUID, Caravan> caravans = new LinkedHashMap<>();
    private final Map<UUID, Vassal> vassals = new HashMap<>();
    /** Players who betrayed the king: the horde never takes them back. */
    private final Set<UUID> traitors = new HashSet<>();

    public static GoblinRaidData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(GoblinRaidData::new, GoblinRaidData::load), NAME);
    }

    // ---------------------------------------------------------------- bases

    /** Chests of one player (overworld positions, oldest first) and raid timing. */
    public static final class PlayerBase {
        private final LinkedHashSet<BlockPos> chests = new LinkedHashSet<>();
        /** Game time of the last raid; when never raided, the time the first chest was tracked. */
        private long lastRaid;
        private int raids;

        private PlayerBase(long lastRaid) {
            this.lastRaid = lastRaid;
        }

        public List<BlockPos> chests() {
            return List.copyOf(this.chests);
        }

        public long lastRaid() {
            return this.lastRaid;
        }

        public int raids() {
            return this.raids;
        }
    }

    @Nullable
    public PlayerBase base(UUID player) {
        return this.bases.get(player);
    }

    public void trackChest(UUID player, BlockPos pos, long gameTime) {
        PlayerBase base = this.bases.computeIfAbsent(player, id -> new PlayerBase(gameTime));
        BlockPos key = pos.immutable();
        if (base.chests.remove(key)) {
            base.chests.add(key); // most recently used last
            return;
        }
        base.chests.add(key);
        while (base.chests.size() > MAX_TRACKED_CHESTS) {
            base.chests.remove(base.chests.iterator().next());
        }
        this.setDirty();
    }

    public void untrackChest(BlockPos pos) {
        for (PlayerBase base : this.bases.values()) {
            if (base.chests.remove(pos)) {
                this.setDirty();
            }
        }
    }

    public void markRaided(UUID player, long gameTime) {
        PlayerBase base = this.bases.get(player);
        if (base != null) {
            base.lastRaid = gameTime;
            base.raids++;
            this.setDirty();
        }
    }

    // ---------------------------------------------------------------- stolen loot

    public void addStolen(ChunkPos village, List<ItemStack> items) {
        List<ItemStack> list = this.stolen.computeIfAbsent(village.toLong(), k -> new ArrayList<>());
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                list.add(stack.copy());
            }
        }
        this.setDirty();
    }

    /** Removes and returns everything the village holds. */
    public List<ItemStack> takeStolen(ChunkPos village) {
        List<ItemStack> list = this.stolen.remove(village.toLong());
        if (list == null) {
            return List.of();
        }
        this.setDirty();
        return list;
    }

    public int stolenStacks(ChunkPos village) {
        List<ItemStack> list = this.stolen.get(village.toLong());
        return list == null ? 0 : list.size();
    }

    // ---------------------------------------------------------------- caravans

    public void addCaravan(Caravan caravan) {
        this.caravans.put(caravan.id, caravan);
        this.setDirty();
    }

    public Collection<Caravan> caravans() {
        return this.caravans.values();
    }

    @Nullable
    public Caravan caravan(UUID id) {
        return this.caravans.get(id);
    }

    public void removeCaravan(UUID id) {
        if (this.caravans.remove(id) != null) {
            this.setDirty();
        }
    }

    // ---------------------------------------------------------------- the king's horde

    /** A player who swore allegiance to the goblin king. */
    public static final class Vassal {
        /** Game time the next tribute is due. */
        public long nextTribute;
        /** Until this game time the horde does not renew the player's disguise (they struck a goblin). */
        public long disgracedUntil;
        /** Whether the "tribute is overdue" warning was already sent for the current period. */
        public boolean warned;

        Vassal(long nextTribute) {
            this.nextTribute = nextTribute;
        }
    }

    @Nullable
    public Vassal vassal(UUID player) {
        return this.vassals.get(player);
    }

    public Vassal addVassal(UUID player, long nextTribute) {
        Vassal vassal = new Vassal(nextTribute);
        this.vassals.put(player, vassal);
        this.setDirty();
        return vassal;
    }

    public void removeVassal(UUID player) {
        if (this.vassals.remove(player) != null) {
            this.setDirty();
        }
    }

    public boolean isTraitor(UUID player) {
        return this.traitors.contains(player);
    }

    public void markTraitor(UUID player) {
        if (this.traitors.add(player)) {
            this.setDirty();
        }
    }

    // ---------------------------------------------------------------- saving

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag bases = new ListTag();
        this.bases.forEach((player, base) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", player);
            entry.putLongArray("Chests", base.chests.stream().mapToLong(BlockPos::asLong).toArray());
            entry.putLong("LastRaid", base.lastRaid);
            entry.putInt("Raids", base.raids);
            bases.add(entry);
        });
        tag.put("Bases", bases);

        ListTag villages = new ListTag();
        this.stolen.forEach((village, items) -> {
            CompoundTag entry = new CompoundTag();
            entry.putLong("Village", village);
            ListTag list = new ListTag();
            for (ItemStack stack : items) {
                list.add(stack.save(registries));
            }
            entry.put("Items", list);
            villages.add(entry);
        });
        tag.put("Stolen", villages);

        ListTag caravanTags = new ListTag();
        this.caravans.values().forEach(c -> caravanTags.add(c.save(registries)));
        tag.put("Caravans", caravanTags);

        ListTag vassalTags = new ListTag();
        this.vassals.forEach((player, vassal) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", player);
            entry.putLong("NextTribute", vassal.nextTribute);
            entry.putLong("DisgracedUntil", vassal.disgracedUntil);
            entry.putBoolean("Warned", vassal.warned);
            vassalTags.add(entry);
        });
        tag.put("Vassals", vassalTags);

        ListTag traitorTags = new ListTag();
        this.traitors.forEach(player -> traitorTags.add(NbtUtils.createUUID(player)));
        tag.put("Traitors", traitorTags);
        return tag;
    }

    private static GoblinRaidData load(CompoundTag tag, HolderLookup.Provider registries) {
        GoblinRaidData data = new GoblinRaidData();
        for (Tag t : tag.getList("Bases", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            PlayerBase base = new PlayerBase(entry.getLong("LastRaid"));
            base.raids = entry.getInt("Raids");
            for (long pos : entry.getLongArray("Chests")) {
                base.chests.add(BlockPos.of(pos));
            }
            data.bases.put(entry.getUUID("Player"), base);
        }
        for (Tag t : tag.getList("Stolen", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            List<ItemStack> items = new ArrayList<>();
            for (Tag item : entry.getList("Items", Tag.TAG_COMPOUND)) {
                ItemStack.parse(registries, item).ifPresent(items::add);
            }
            data.stolen.put(entry.getLong("Village"), items);
        }
        for (Tag t : tag.getList("Caravans", Tag.TAG_COMPOUND)) {
            Caravan caravan = Caravan.load((CompoundTag) t, registries);
            data.caravans.put(caravan.id, caravan);
        }
        for (Tag t : tag.getList("Vassals", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            Vassal vassal = new Vassal(entry.getLong("NextTribute"));
            vassal.disgracedUntil = entry.getLong("DisgracedUntil");
            vassal.warned = entry.getBoolean("Warned");
            data.vassals.put(entry.getUUID("Player"), vassal);
        }
        for (Tag t : tag.getList("Traitors", Tag.TAG_INT_ARRAY)) {
            data.traitors.add(NbtUtils.loadUUID(t));
        }
        return data;
    }
}
