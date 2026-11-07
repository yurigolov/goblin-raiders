package com.goblinraiders.raid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * Goblins carrying a raid's loot from the robbed base to their village. The record is the single source of truth:
 * while nobody is near, the caravan just moves along on paper; when a player comes close it turns into real goblins
 * (see {@link Caravans}), each loot carrier holding one share. Killing a carrier drops its share.
 */
public final class Caravan {
    public final UUID id;
    public final UUID owner;
    /** Village center (the structure start chunk corner); y is unused. */
    public final BlockPos village;
    public final long startTime;
    public double x;
    public double z;
    /** Loot, one list per carrier; a share becomes empty when its carrier is killed. */
    public final List<List<ItemStack>> shares;
    public boolean materialized;
    /** Goblin entities of the materialized caravan. */
    public final List<UUID> members = new ArrayList<>();
    /** Game time a player was last near the materialized caravan. */
    public long lastWatched;

    public Caravan(UUID id, UUID owner, BlockPos village, long startTime, double x, double z, List<List<ItemStack>> shares) {
        this.id = id;
        this.owner = owner;
        this.village = village;
        this.startTime = startTime;
        this.x = x;
        this.z = z;
        this.shares = shares;
    }

    public boolean hasLoot() {
        return this.shares.stream().anyMatch(share -> !share.isEmpty());
    }

    public List<ItemStack> allLoot() {
        List<ItemStack> all = new ArrayList<>();
        this.shares.forEach(all::addAll);
        return all;
    }

    public double distanceToVillage() {
        double dx = this.village.getX() + 0.5 - this.x, dz = this.village.getZ() + 0.5 - this.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", this.id);
        tag.putUUID("Owner", this.owner);
        tag.putLong("Village", this.village.asLong());
        tag.putLong("Start", this.startTime);
        tag.putDouble("X", this.x);
        tag.putDouble("Z", this.z);
        ListTag shares = new ListTag();
        for (List<ItemStack> share : this.shares) {
            ListTag items = new ListTag();
            for (ItemStack stack : share) {
                items.add(stack.save(registries));
            }
            CompoundTag entry = new CompoundTag();
            entry.put("Items", items);
            shares.add(entry);
        }
        tag.put("Shares", shares);
        // Members are never saved: after a reload the caravan simply continues on paper.
        return tag;
    }

    static Caravan load(CompoundTag tag, HolderLookup.Provider registries) {
        List<List<ItemStack>> shares = new ArrayList<>();
        for (Tag t : tag.getList("Shares", Tag.TAG_COMPOUND)) {
            List<ItemStack> share = new ArrayList<>();
            for (Tag item : ((CompoundTag) t).getList("Items", Tag.TAG_COMPOUND)) {
                ItemStack.parse(registries, item).ifPresent(share::add);
            }
            shares.add(share);
        }
        return new Caravan(tag.getUUID("Id"), tag.getUUID("Owner"), BlockPos.of(tag.getLong("Village")), tag.getLong("Start"),
                tag.getDouble("X"), tag.getDouble("Z"), shares);
    }

    static Map<UUID, Caravan> index(List<Caravan> caravans) {
        Map<UUID, Caravan> map = new HashMap<>();
        caravans.forEach(c -> map.put(c.id, c));
        return map;
    }
}
