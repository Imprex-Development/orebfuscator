package net.imprex.orebfuscator.obfuscation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.World;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.chunk.Chunk;
import net.imprex.orebfuscator.chunk.ChunkSection;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.BlockFlags;
import net.imprex.orebfuscator.config.ObfuscationConfig;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.ProximityConfig;
import net.imprex.orebfuscator.config.ProximityHeightCondition;
import net.imprex.orebfuscator.config.WorldConfigBundle;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.HeightAccessor;

public class ObfuscationTaskProcessorOld {

	private final OrebfuscatorConfig config;

	public ObfuscationTaskProcessorOld(Orebfuscator orebfuscator) {
		this.config = orebfuscator.getOrebfuscatorConfig();
	}

	public int process(ObfuscationTask task) {
		ChunkStruct chunkStruct = task.getChunkStruct();

		World world = chunkStruct.world;
		HeightAccessor heightAccessor = HeightAccessor.get(world);

		WorldConfigBundle bundle = this.config.world(world);
		BlockFlags blockFlags = bundle.blockFlags();
		ObfuscationConfig obfuscationConfig = bundle.obfuscation();
		ProximityConfig proximityConfig = bundle.proximity();

		Set<BlockPos> blockEntities = new HashSet<>();
		List<BlockPos> proximityBlocks = new ArrayList<>();

		int baseX = chunkStruct.chunkX << 4;
		int baseZ = chunkStruct.chunkZ << 4;

		int layerY = Integer.MIN_VALUE;
		int layerYBlockState = -1;

		try (Chunk chunk = Chunk.fromChunkStruct(chunkStruct)) {
			for (int sectionIndex = Math.max(0, bundle.minSectionIndex()); sectionIndex <= Math
					.min(chunk.getSectionCount() - 1, bundle.maxSectionIndex()); sectionIndex++) {
				ChunkSection chunkSection = chunk.getSection(sectionIndex);
				if (chunkSection == null || chunkSection.isEmpty()) {
					continue;
				}

				final int baseY = heightAccessor.getMinBuildHeight() + (sectionIndex << 4);
				for (int index = 0; index < 4096; index++) {
					int y = baseY + (index >> 8 & 15);
					if (!bundle.shouldProcessBlock(y)) {
						continue;
					}

					int blockState = chunkSection.getBlockState(index);

					int obfuscateBits = blockFlags.flags(blockState, y);
					if (BlockFlags.isEmpty(obfuscateBits)) {
						continue;
					}

					int x = baseX + (index & 15);
					int z = baseZ + (index >> 4 & 15);

					boolean isObfuscateBitSet = BlockFlags.isObfuscateBitSet(obfuscateBits);
					boolean obfuscated = false;

					// should current block be obfuscated
					if (isObfuscateBitSet && obfuscationConfig.shouldObfuscate(y) && shouldObfuscate(task, chunk, x, y, z)) {
						if (obfuscationConfig.layerObfuscation()) {
							if (layerY != y) {
								layerY = y;
								layerYBlockState = bundle.nextRandomObfuscationBlock(y);
							}
							blockState = layerYBlockState;
						} else {
							blockState = bundle.nextRandomObfuscationBlock(y);
						}
						obfuscated = true;
					}

					// should current block be proximity hidden
					if (!obfuscated && BlockFlags.isProximityBitSet(obfuscateBits) && proximityConfig.shouldObfuscate(y)) {
						proximityBlocks.add(new BlockPos(x, y, z));
						if (BlockFlags.isUseBlockBelowBitSet(obfuscateBits)) {
							boolean allowNonOcclude = !isObfuscateBitSet || !ProximityHeightCondition.isPresent(obfuscateBits);
							blockState = getBlockStateBelow(bundle, chunk, x, y, z, allowNonOcclude);
						} else {
							blockState = bundle.nextRandomProximityBlock(y);
						}
						obfuscated = true;
					}

					// update block state if needed
					if (obfuscated) {
						chunkSection.setBlockState(index, blockState);
						if (BlockFlags.isBlockEntityBitSet(obfuscateBits)) {
							blockEntities.add(new BlockPos(x, y, z));
						}
					}
				}
			}

			byte[] output = chunk.finalizeOutput();
			task.complete(output, blockEntities, proximityBlocks);
			return output.length;
		} catch (Exception e) {
			task.completeExceptionally(e);
			return 0;
		}
	}

	// returns first block below given position that wouldn't be obfuscated in any
	// way at given position
	private int getBlockStateBelow(WorldConfigBundle bundle, Chunk chunk, int x, int y, int z, boolean allowNonOcclude) {
		BlockFlags blockFlags = bundle.blockFlags();

		for (int targetY = y - 1; targetY > chunk.getHeightAccessor().getMinBuildHeight(); targetY--) {
			int blockData = chunk.getBlockState(x, targetY, z);
			if (blockData != -1 && (allowNonOcclude || OrebfuscatorNms.isOccluding(blockData))) {
				int mask = blockFlags.flags(blockData, y);
				if (BlockFlags.isEmpty(mask) || BlockFlags.isAllowForUseBlockBelowBitSet(mask)) {
					return blockData;
				}
			}
		}

		return bundle.nextRandomProximityBlock(y);
	}

	private boolean shouldObfuscate(ObfuscationTask task, Chunk chunk, int x, int y, int z) {
		return isAdjacentBlockOccluding(task, chunk, x, y + 1, z)
				&& isAdjacentBlockOccluding(task, chunk, x, y - 1, z)
				&& isAdjacentBlockOccluding(task, chunk, x + 1, y, z)
				&& isAdjacentBlockOccluding(task, chunk, x - 1, y, z)
				&& isAdjacentBlockOccluding(task, chunk, x, y, z + 1)
				&& isAdjacentBlockOccluding(task, chunk, x, y, z - 1);
	}

	private boolean isAdjacentBlockOccluding(ObfuscationTask task, Chunk chunk, int x, int y, int z) {
		if (y >= chunk.getHeightAccessor().getMaxBuildHeight() || y < chunk.getHeightAccessor().getMinBuildHeight()) {
			return false;
		}

		int blockId = chunk.getBlockState(x, y, z);
		if (blockId == -1) {
			blockId = task.getBlockState(x, y, z);
		}

		return blockId >= 0 && OrebfuscatorNms.isOccluding(blockId);
	}
}
