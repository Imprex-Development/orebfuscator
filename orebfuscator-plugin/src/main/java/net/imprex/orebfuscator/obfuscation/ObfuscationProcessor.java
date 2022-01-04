package net.imprex.orebfuscator.obfuscation;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.World;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.chunk.Chunk;
import net.imprex.orebfuscator.chunk.ChunkSection;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.BlockFlags;
import net.imprex.orebfuscator.config.ObfuscationConfig;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.ProximityConfig;
import net.imprex.orebfuscator.config.WorldConfigBundle;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.HeightAccessor;

public class ObfuscationProcessor {

	private final OrebfuscatorConfig config;

	public ObfuscationProcessor(Orebfuscator orebfuscator) {
		this.config = orebfuscator.getOrebfuscatorConfig();
	}

	public void process(ObfuscationTask task) {
		ChunkStruct chunkStruct = task.getChunkStruct();

		World world = chunkStruct.world;
		HeightAccessor heightAccessor = HeightAccessor.get(world);

		WorldConfigBundle bundle = this.config.bundle(world);
		BlockFlags blockFlags = bundle.blockFlags();
		ObfuscationConfig obfuscationConfig = bundle.obfuscation();
		ProximityConfig proximityConfig = bundle.proximity();

		Set<BlockPos> blockEntities = new HashSet<>();
		Set<BlockPos> proximityBlocks = new HashSet<>();

		int baseX = chunkStruct.chunkX << 4;
		int baseZ = chunkStruct.chunkZ << 4;

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
					if (!bundle.shouldObfuscate(y)) {
						continue;
					}

					int blockState = chunkSection.getBlock(index);

					int obfuscateBits = blockFlags.flags(blockState, y);
					if (BlockFlags.isEmpty(obfuscateBits)) {
						continue;
					}

					int x = baseX + (index & 15);
					int z = baseZ + (index >> 4 & 15);

					boolean obfuscated = false;

					// should current block be obfuscated
					if (BlockFlags.isObfuscateBitSet(obfuscateBits) && shouldObfuscate(task, chunk, x, y, z)
							&& obfuscationConfig.shouldObfuscate(y)) {
						blockState = obfuscationConfig.nextRandomBlockState();
						obfuscated = true;
					}

					// should current block be proximity hidden
					if (!obfuscated && BlockFlags.isProximityBitSet(obfuscateBits) && proximityConfig.shouldObfuscate(y)) {
						proximityBlocks.add(new BlockPos(x, y, z));
						if (BlockFlags.isUseBlockBelowBitSet(obfuscateBits)) {
							blockState = getBlockBelow(blockFlags, chunk, x, y, z);
						} else {
							blockState = proximityConfig.nextRandomBlockState();
						}
						obfuscated = true;
					}

					// update block state if needed
					if (obfuscated) {
						chunkSection.setBlock(index, blockState);
						if (BlockFlags.isBlockEntityBitSet(obfuscateBits)) {
							blockEntities.add(new BlockPos(x, y, z));
						}
					}
				}
			}

			task.complete(chunk.finalizeOutput(), blockEntities, proximityBlocks);
		} catch (Exception e) {
			task.completeExceptionally(e);
		}
	}

	// returns first block below given position that wouldn't be obfuscated in any
	// way at given position
	private int getBlockBelow(BlockFlags blockFlags, Chunk chunk, int x, int y, int z) {
		for (int targetY = y - 1; targetY > chunk.getHeightAccessor().getMinBuildHeight(); targetY--) {
			int blockData = chunk.getBlock(x, targetY, z);
			if (blockData != -1 && BlockFlags.isEmpty(blockFlags.flags(blockData, y))) {
				return blockData;
			}
		}
		return 0;
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

		int blockId = chunk.getBlock(x, y, z);
		if (blockId == -1) {
			blockId = task.getBlockState(x, y, z);
		}

		return blockId >= 0 && NmsInstance.isOccluding(blockId);
	}
}
