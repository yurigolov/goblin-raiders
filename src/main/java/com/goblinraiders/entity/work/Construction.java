package com.goblinraiders.entity.work;

import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.block.BuildingSiteBlock;
import com.goblinraiders.block.BuildingSiteBlockEntity;
import com.goblinraiders.entity.GoblinVillager;
import com.goblinraiders.registry.ModEntities;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Goblin builders put up new buildings on building sites ({@link BuildingSiteBlock}), block by block, after a blueprint
 * from {@code data/<ns>/goblin_blueprint/<name>.json} (written by tools/structures/goblin_village.py). Wooden blocks cost
 * wood (a log is worth four, a plank one), stone blocks cost cobblestone; everything else is free. When a house is
 * finished, a new goblin family moves in.
 */
public final class Construction {
    public enum Material { NONE, WOOD, STONE }

    /** One block of a blueprint, in blueprint coordinates (center of the plot at x = z = 0, ground at y = 0). */
    public record Part(BlockPos pos, BlockState state) {
    }

    /** A blueprint: the blocks in building order, and whether a family moves in when it is done. */
    public record Blueprint(String name, List<Part> parts, boolean settlers) {
    }

    /** The next block to put up on a site. */
    public record Step(BlockPos pos, BlockState state, Material material) {
    }

    public static final List<String> BLUEPRINTS = List.of("hut_small", "hut_big", "garden");
    /** Settlers never fill a village beyond this. */
    private static final int MAX_SETTLERS = 24;
    private static final Map<String, Blueprint> CACHE = new HashMap<>();

    // ---------------------------------------------------------------- blueprints

    @Nullable
    public static Blueprint blueprint(ServerLevel level, String name) {
        if (CACHE.containsKey(name)) {
            return CACHE.get(name);
        }
        Blueprint blueprint = load(level, name);
        CACHE.put(name, blueprint);
        return blueprint;
    }

    @Nullable
    private static Blueprint load(ServerLevel level, String name) {
        var resource = level.getServer().getResourceManager().getResource(GoblinRaiders.id("goblin_blueprint/" + name + ".json"));
        if (resource.isEmpty()) {
            GoblinRaiders.LOGGER.warn("No goblin blueprint {}", name);
            return null;
        }
        try (Reader reader = resource.get().openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            var blocks = level.holderLookup(Registries.BLOCK);
            List<Part> parts = new ArrayList<>();
            for (JsonElement element : json.getAsJsonArray("blocks")) {
                JsonArray a = element.getAsJsonArray();
                BlockState state = BlockStateParser.parseForBlock(blocks, a.get(3).getAsString(), false).blockState();
                parts.add(new Part(new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt()), state));
            }
            // Clear plants first, then the solid blocks bottom up, then everything that hangs on them.
            parts.sort(Comparator.<Part>comparingInt(p -> p.state.isAir() ? 0 : p.state.canOcclude() ? 1 : 2)
                    .thenComparingInt(p -> p.pos.getY()).thenComparingInt(p -> p.pos.getX()).thenComparingInt(p -> p.pos.getZ()));
            return new Blueprint(name, parts, json.has("settlers") && json.get("settlers").getAsBoolean());
        } catch (Exception e) {
            GoblinRaiders.LOGGER.error("Broken goblin blueprint {}", name, e);
            return null;
        }
    }

    /** Quarter turns of a site: the blueprints are drawn with the door to the south. */
    public static Rotation rotation(Direction facing) {
        return switch (facing) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static BlockPos worldPos(BlockPos site, Rotation rotation, BlockPos part) {
        BlockPos turned = part.rotate(rotation);
        return site.offset(turned.getX(), part.getY() - 1, turned.getZ());   // the stake stands on the ground (blueprint y = 1)
    }

    // ---------------------------------------------------------------- materials

    public static Material material(BlockState state) {
        if (state.isAir()) {
            return Material.NONE;
        }
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_STAIRS) || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_FENCES) || state.is(BlockTags.FENCE_GATES) || state.is(BlockTags.WOODEN_DOORS)
                || state.is(BlockTags.WOODEN_TRAPDOORS)) {
            return Material.WOOD;
        }
        String path = BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath();
        if (path.contains("cobblestone") || path.contains("stone_brick") || path.equals("stone") || path.contains("mud_brick")
                || path.equals("packed_mud")) {
            return Material.STONE;
        }
        return Material.NONE;
    }

    /** How much of a material an item is worth (0 if none). */
    public static int worth(ItemStack stack, Material material) {
        return switch (material) {
            case WOOD -> stack.is(ItemTags.LOGS) ? 4 : stack.is(ItemTags.PLANKS) ? 1 : 0;
            case STONE -> stack.is(Items.COBBLESTONE) || stack.is(Items.COBBLED_DEEPSLATE) || stack.is(Items.STONE) ? 1 : 0;
            case NONE -> 0;
        };
    }

    public static boolean isMaterial(ItemStack stack) {
        return worth(stack, Material.WOOD) > 0 || worth(stack, Material.STONE) > 0;
    }

    // ---------------------------------------------------------------- building

    /** The nearest unfinished building site within {@code radius} of {@code center} (in the loaded chunks). */
    @Nullable
    public static BlockPos findSite(Level level, BlockPos center, int radius) {
        BlockPos best = null;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 4, radius))) {
            if (level.getBlockState(pos).getBlock() instanceof BuildingSiteBlock && (best == null || pos.distSqr(center) < best.distSqr(center))) {
                best = pos.immutable();
            }
        }
        return best;
    }

    /** Whether the builder may put {@code state} where {@code current} is: air, plants, snow, or the ground for floors. */
    private static boolean canReplace(BlockState current, BlockState state, int y) {
        if (current.isAir() || current.canBeReplaced()) {
            return true;
        }
        return y <= 1 && !state.isAir() && (current.is(BlockTags.DIRT) || current.is(Blocks.GRAVEL) || current.is(Blocks.SAND)
                || current.is(Blocks.DIRT_PATH));
    }

    /**
     * The next block the site needs, moving its progress past blocks that are already right or cannot be placed
     * (a player put something there). Null when the blueprint is done.
     */
    @Nullable
    public static Step nextStep(ServerLevel level, BlockPos site) {
        if (!(level.getBlockEntity(site) instanceof BuildingSiteBlockEntity entity)) {
            return null;
        }
        Blueprint blueprint = blueprint(level, entity.getBlueprint());
        if (blueprint == null) {
            return null;
        }
        Rotation rotation = rotation(level.getBlockState(site).getValue(BuildingSiteBlock.FACING));
        while (entity.getProgress() < blueprint.parts().size()) {
            Part part = blueprint.parts().get(entity.getProgress());
            BlockPos pos = worldPos(site, rotation, part.pos());
            BlockState want = part.state().rotate(rotation);
            BlockState current = level.getBlockState(pos);
            boolean clearing = want.isAir();
            if (!pos.equals(site) && !current.equals(want) && (clearing ? !current.isAir() && current.canBeReplaced() : canReplace(current, want, part.pos().getY()))) {
                return new Step(pos, want, material(want));
            }
            entity.advance();
        }
        return null;
    }

    /** Puts up one block of the site (the builder has already paid for it). */
    public static void place(ServerLevel level, BlockPos site, Step step) {
        level.setBlock(step.pos(), step.state(), 2);
        var sound = step.state().getSoundType();
        level.playSound(null, step.pos(), sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        if (level.getBlockEntity(site) instanceof BuildingSiteBlockEntity entity) {
            entity.advance();
        }
    }

    /** The site is done: the stake goes, a family moves into a new house and everybody around hears about it. */
    public static void finish(ServerLevel level, BlockPos site) {
        if (!(level.getBlockEntity(site) instanceof BuildingSiteBlockEntity entity)) {
            return;
        }
        Blueprint blueprint = blueprint(level, entity.getBlueprint());
        Rotation rotation = rotation(level.getBlockState(site).getValue(BuildingSiteBlock.FACING));
        BlockState here = Blocks.AIR.defaultBlockState();
        if (blueprint != null) {
            for (Part part : blueprint.parts()) {
                if (worldPos(site, rotation, part.pos()).equals(site)) {
                    here = part.state().rotate(rotation);
                }
            }
        }
        level.setBlock(site, here, 3);
        boolean settlers = blueprint != null && blueprint.settlers();
        if (settlers && level.getEntitiesOfClass(GoblinVillager.class, new AABB(site).inflate(48.0)).size() < MAX_SETTLERS) {
            GoblinVillager settler = ModEntities.GOBLIN_VILLAGER.get().create(level);
            if (settler != null) {
                settler.moveTo(site.getX() + 0.5, site.getY(), site.getZ() + 0.5, level.random.nextFloat() * 360.0F, 0.0F);
                settler.finalizeSpawn(level, level.getCurrentDifficultyAt(site), MobSpawnType.EVENT, null);
                settler.setJob(GoblinVillager.newcomerJob(level.random));
                settler.setHome(site);
                settler.setPersistenceRequired();
                level.addFreshEntity(settler);
            }
        }
        String key = settlers ? "goblinraiders.hamlet.house_built" : "goblinraiders.hamlet.built";
        Component name = Component.translatable("goblinraiders.blueprint." + entity.getBlueprint());
        for (ServerPlayer player : level.getPlayers(p -> p.distanceToSqr(site.getX(), site.getY(), site.getZ()) < 48 * 48)) {
            player.sendSystemMessage(Component.translatable(key, name).withStyle(ChatFormatting.DARK_GREEN));
            GoblinAdvancements.award(player, GoblinAdvancements.NEW_HOUSE);
        }
    }

    /** "Building «Hut»: 35 of 140 blocks." */
    public static Component describe(ServerLevel level, BlockPos site) {
        if (!(level.getBlockEntity(site) instanceof BuildingSiteBlockEntity entity)) {
            return Component.empty();
        }
        Blueprint blueprint = blueprint(level, entity.getBlueprint());
        int total = blueprint == null ? 0 : blueprint.parts().size();
        return Component.translatable("goblinraiders.hamlet.site", Component.translatable("goblinraiders.blueprint." + entity.getBlueprint()),
                Math.min(entity.getProgress(), total), total);
    }

    private Construction() {
    }
}
