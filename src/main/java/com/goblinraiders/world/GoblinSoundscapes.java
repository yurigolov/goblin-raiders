package com.goblinraiders.world;

import com.goblinraiders.Config;
import com.goblinraiders.GoblinRaiders;
import com.goblinraiders.advancement.GoblinAdvancements;
import com.goblinraiders.entity.GoblinNight;
import com.goblinraiders.registry.ModSounds;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * The sound of the goblin settlements. Once a second, a player standing in one hears now and then a sound of that
 * place from somewhere around: chatter and laughter, a hammer, a grindstone, coins at the market, a pipe and birds in a
 * peaceful hamlet, crickets and owls at night. War camps and the goblin city beat their war drums from the middle of the
 * camp in the daytime, loud enough to be heard before one gets there; a hamlet's bell rings in the morning. The server
 * plays every sound at a spot in the world, so everyone around hears it from there.
 */
@EventBusSubscriber(modid = GoblinRaiders.MODID)
public final class GoblinSoundscapes {
    public enum Place { CAMP, VILLAGE, MARKET, WAR_CAMP, CITY, HAMLET, SAGE }

    /** A sound of a place: how often it comes up against the others and how loud. */
    public record Cue(Supplier<SoundEvent> sound, int weight, float volume) {
    }

    private static final Map<String, Place> STRUCTURES = Map.of(
            "goblin_camp", Place.CAMP, "goblin_village", Place.VILLAGE, "goblin_market", Place.MARKET,
            "goblin_warcamp", Place.WAR_CAMP, "goblin_megavillage", Place.CITY, "goblin_hamlet", Place.HAMLET,
            "goblin_sage_tent", Place.SAGE);
    private static final Map<Place, List<Cue>> DAY = new EnumMap<>(Place.class);
    private static final Map<Place, List<Cue>> NIGHT = new EnumMap<>(Place.class);

    static {
        Cue chatter = new Cue(ModSounds.CHATTER, 4, 0.7F);
        Cue chop = new Cue(ModSounds.CHOP, 2, 0.6F);
        Cue forge = new Cue(ModSounds.FORGE, 2, 0.5F);
        Cue sharpen = new Cue(ModSounds.SHARPEN, 2, 0.5F);
        Cue coins = new Cue(ModSounds.COINS, 3, 0.6F);
        Cue farm = new Cue(ModSounds.FARM, 3, 0.6F);
        Cue pipe = new Cue(ModSounds.PIPE, 2, 0.7F);
        Cue birds = new Cue(ModSounds.BIRDS, 3, 0.6F);
        Cue mystic = new Cue(ModSounds.MYSTIC, 2, 0.5F);
        Cue crickets = new Cue(ModSounds.CRICKETS, 3, 0.6F);
        Cue owl = new Cue(ModSounds.OWL, 1, 0.8F);
        Cue murmur = new Cue(ModSounds.CHATTER, 1, 0.35F);   // the watchmen by the fire
        DAY.put(Place.CAMP, List.of(chatter, chop, forge));
        DAY.put(Place.VILLAGE, List.of(chatter, chop, forge, new Cue(ModSounds.FARM, 1, 0.5F)));
        DAY.put(Place.MARKET, List.of(new Cue(ModSounds.CHATTER, 5, 0.7F), coins, new Cue(ModSounds.FORGE, 1, 0.5F)));
        DAY.put(Place.WAR_CAMP, List.of(new Cue(ModSounds.FORGE, 3, 0.6F), new Cue(ModSounds.SHARPEN, 3, 0.6F),
                new Cue(ModSounds.CHATTER, 2, 0.7F), new Cue(ModSounds.CHOP, 1, 0.6F)));
        DAY.put(Place.CITY, List.of(chatter, forge, new Cue(ModSounds.COINS, 2, 0.6F), new Cue(ModSounds.SHARPEN, 1, 0.5F),
                new Cue(ModSounds.CHOP, 1, 0.6F)));
        DAY.put(Place.HAMLET, List.of(birds, pipe, farm, new Cue(ModSounds.CHOP, 1, 0.6F), new Cue(ModSounds.FORGE, 1, 0.4F),
                new Cue(ModSounds.CHATTER, 1, 0.6F)));
        DAY.put(Place.SAGE, List.of(new Cue(ModSounds.BIRDS, 2, 0.6F), mystic));
        for (Place place : List.of(Place.CAMP, Place.VILLAGE, Place.CITY)) {
            NIGHT.put(place, List.of(crickets, owl, murmur));
        }
        NIGHT.put(Place.MARKET, List.of(crickets, owl, new Cue(ModSounds.COINS, 1, 0.4F)));   // somebody counting the takings
        NIGHT.put(Place.WAR_CAMP, List.of(new Cue(ModSounds.CRICKETS, 2, 0.6F), new Cue(ModSounds.SHARPEN, 1, 0.4F), owl));
        NIGHT.put(Place.HAMLET, List.of(crickets, new Cue(ModSounds.OWL, 2, 0.8F)));
        NIGHT.put(Place.SAGE, List.of(new Cue(ModSounds.OWL, 2, 0.8F), new Cue(ModSounds.CRICKETS, 2, 0.6F), mystic));
    }

    /** A settlement sound comes about once in this many seconds. */
    private static final int ONE_IN = 4;
    /** Drums carry this far from the middle of the camp. */
    private static final int DRUM_RANGE = 80;
    private static final int HORN_ONE_IN = 4;
    /** Next war drums per camp (by the chunk of its start), and the day each hamlet last rang its bell. */
    private static final Map<Long, Long> NEXT_DRUMS = new HashMap<>();
    private static final Map<Long, Long> BELL_DAY = new HashMap<>();

    /** A goblin settlement the player is in or near. */
    public record Settlement(Place place, BoundingBox box, long key) {
        public BlockPos center() {
            return new BlockPos(this.box.getCenter().getX(), this.box.minY() + 2, this.box.getCenter().getZ());
        }

        public boolean contains(BlockPos pos) {
            return this.box.inflatedBy(4).isInside(pos);
        }
    }

    public static List<Cue> cues(Place place, boolean night) {
        return (night ? NIGHT : DAY).get(place);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (!Config.AMBIENCE.get() || server.getTickCount() % 20 != 0) {
            return;
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isSpectator() && player.level() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
                listen(level, player);
            }
        }
    }

    private static void listen(ServerLevel level, ServerPlayer player) {
        BlockPos pos = player.blockPosition();
        Settlement near = nearest(level, pos);
        if (near == null) {
            return;
        }
        RandomSource random = level.getRandom();
        boolean night = GoblinNight.isNight(level);
        if (!night && (near.place() == Place.WAR_CAMP || near.place() == Place.CITY)) {
            beatDrums(level, near, pos, random);
        }
        if (!near.contains(pos)) {
            return;
        }
        if (near.place() == Place.HAMLET) {
            ringMorningBell(level, near);
        }
        if (random.nextInt(ONE_IN) != 0) {
            return;
        }
        Cue cue = pick(cues(near.place(), night), random);
        if (cue.sound() == ModSounds.BIRDS && level.isRaining()) {
            return;   // birds keep quiet in the rain
        }
        level.playSound(null, spot(level, pos, near, random), cue.sound().get(), SoundSource.AMBIENT, cue.volume(),
                0.9F + random.nextFloat() * 0.2F);
    }

    /** Every minute or so the camp beats its drums (now and then a horn blares instead), heard from far around. */
    private static void beatDrums(ServerLevel level, Settlement camp, BlockPos listener, RandomSource random) {
        BlockPos center = camp.center();
        if (listener.distSqr(center) > Mth.square(DRUM_RANGE - 8)) {
            return;   // out of earshot (a flying player too): keep the drums for when someone can hear them
        }
        long now = level.getGameTime();
        if (now < NEXT_DRUMS.getOrDefault(camp.key(), 0L)) {
            return;
        }
        NEXT_DRUMS.put(camp.key(), now + 20L * (45 + random.nextInt(60)));
        boolean horn = random.nextInt(HORN_ONE_IN) == 0;
        level.playSound(null, center, (horn ? ModSounds.WAR_HORN : ModSounds.WAR_DRUMS).get(), SoundSource.AMBIENT, DRUM_RANGE / 16.0F,
                horn ? 0.9F + random.nextFloat() * 0.2F : 1.0F);
        if (!horn) {
            for (ServerPlayer player : level.players()) {
                if (horizontalDistance(player.blockPosition(), center) <= DRUM_RANGE - 16) {
                    GoblinAdvancements.award(player, GoblinAdvancements.WAR_DRUMS);
                }
            }
        }
    }

    /** The headman rings the bell on the square once in the first minute of the day. */
    private static void ringMorningBell(ServerLevel level, Settlement hamlet) {
        long time = level.getDayTime();
        long day = time / 24000L;
        if (time % 24000L < 1200L && BELL_DAY.getOrDefault(hamlet.key(), -1L) != day) {
            BELL_DAY.put(hamlet.key(), day);
            level.playSound(null, hamlet.center(), ModSounds.BELL.get(), SoundSource.AMBIENT, 3.0F, 1.0F);
        }
    }

    /** The goblin settlement around the player, or the nearest one within three chunks. */
    @Nullable
    public static Settlement nearest(ServerLevel level, BlockPos pos) {
        ChunkPos here = new ChunkPos(pos);
        Settlement best = null;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -3; dx <= 3; dx += 3) {
            for (int dz = -3; dz <= 3; dz += 3) {
                int cx = here.x + dx, cz = here.z + dz;
                if (!level.hasChunk(cx, cz)) {
                    continue;   // never load or generate chunks for a sound
                }
                for (StructureStart start : level.structureManager().startsForStructure(new ChunkPos(cx, cz), s -> place(level, s) != null)) {
                    BoundingBox box = start.getBoundingBox();
                    double distance = box.inflatedBy(4).isInside(pos) ? 0.0 : horizontalDistance(pos, box.getCenter());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = new Settlement(place(level, start.getStructure()), box, start.getChunkPos().toLong());
                    }
                }
            }
        }
        return best;
    }

    @Nullable
    private static Place place(ServerLevel level, Structure structure) {
        ResourceLocation id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
        return id != null && GoblinRaiders.MODID.equals(id.getNamespace()) ? STRUCTURES.get(id.getPath()) : null;
    }

    private static Cue pick(List<Cue> cues, RandomSource random) {
        int roll = random.nextInt(cues.stream().mapToInt(Cue::weight).sum());
        for (Cue cue : cues) {
            roll -= cue.weight();
            if (roll < 0) {
                return cue;
            }
        }
        return cues.get(0);
    }

    /** Somewhere 6 to 18 blocks around the listener, inside the settlement, on the ground (or level with the listener). */
    private static BlockPos spot(ServerLevel level, BlockPos listener, Settlement settlement, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2.0;
        double distance = 6.0 + random.nextDouble() * 12.0;
        BoundingBox box = settlement.box();
        int x = Mth.clamp(listener.getX() + Mth.floor(Math.cos(angle) * distance), box.minX(), box.maxX());
        int z = Mth.clamp(listener.getZ() + Mth.floor(Math.sin(angle) * distance), box.minZ(), box.maxZ());
        int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        return new BlockPos(x, Mth.clamp(ground, listener.getY() - 6, listener.getY() + 6), z);
    }

    private static double horizontalDistance(BlockPos a, BlockPos b) {
        return Math.sqrt(Mth.square((double) a.getX() - b.getX()) + Mth.square((double) a.getZ() - b.getZ()));
    }

    private GoblinSoundscapes() {
    }
}
