package com.goblinraiders.world;

import com.goblinraiders.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;

/**
 * The peaceful village: an ordinary jigsaw structure (same JSON fields as {@code minecraft:jigsaw}) that also puts out
 * the lava it was built over. Surface lava lakes generate before structures, so without this a hamlet now and then
 * stands in a lava pool and burns down within minutes. Lava under and around each piece turns to stone.
 */
public class HamletStructure extends Structure {
    public static final MapCodec<HamletStructure> CODEC = JigsawStructure.CODEC.xmap(HamletStructure::new, hamlet -> hamlet.jigsaw);
    /** How far around a piece lava is put out: lava flows three blocks and sets things alight a few blocks off. */
    private static final int LAVA_MARGIN = 6;

    private final JigsawStructure jigsaw;

    private HamletStructure(JigsawStructure jigsaw) {
        super(new StructureSettings(jigsaw.biomes(), jigsaw.spawnOverrides(), jigsaw.step(), jigsaw.terrainAdaptation()));
        this.jigsaw = jigsaw;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        return this.jigsaw.findGenerationPoint(context);
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.HAMLET.get();
    }

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
                           BoundingBox chunkBox, ChunkPos chunkPos, PiecesContainer pieces) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (StructurePiece piece : pieces.pieces()) {
            BoundingBox box = piece.getBoundingBox();
            int minX = Math.max(chunkBox.minX(), box.minX() - LAVA_MARGIN);
            int maxX = Math.min(chunkBox.maxX(), box.maxX() + LAVA_MARGIN);
            int minZ = Math.max(chunkBox.minZ(), box.minZ() - LAVA_MARGIN);
            int maxZ = Math.min(chunkBox.maxZ(), box.maxZ() + LAVA_MARGIN);
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    for (int y = box.minY() - 4; y <= box.maxY() + 2; y++) {
                        pos.set(x, y, z);
                        if (level.getFluidState(pos).is(FluidTags.LAVA)) {
                            // Pools fill up with stone to the brim; lava running down from above just goes.
                            level.setBlock(pos, y <= box.minY() + 1 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
    }
}
