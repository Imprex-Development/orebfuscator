package net.imprex.orebfuscator.obfuscation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.bukkit.Bukkit;
import org.bukkit.World;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.chunk.next.Chunk;
import net.imprex.orebfuscator.chunk.next.ChunkSection;
import net.imprex.orebfuscator.chunk.next.ChunkWriter;
import net.imprex.orebfuscator.chunk.next.palette.Palette;
import net.imprex.orebfuscator.chunk.next.strategies.BlockStrategy;
import net.imprex.orebfuscator.config.BlockFlags;
import net.imprex.orebfuscator.config.ObfuscationConfig;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.ProximityConfig;
import net.imprex.orebfuscator.config.WorldConfigBundle;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.HeightAccessor;

public class ObfuscationTaskProcessor {

	private static final AtomicLong TIME = new AtomicLong();
	private static final AtomicInteger COUNT = new AtomicInteger(-2500);

	private final OrebfuscatorConfig config;

	public ObfuscationTaskProcessor(Orebfuscator orebfuscator) {
		this.config = orebfuscator.getOrebfuscatorConfig();
	}

	public void process(ObfuscationTask task) {
		long time = System.currentTimeMillis();

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

		int[] blockStates = new int[4096];

		try (Chunk chunk = Chunk.fromChunkStruct(chunkStruct, bundle)) {
			ChunkWriter writer = chunk.getWriter();
			while (writer.hasNext()) {
				try {
					ChunkSection chunkSection = writer.getOrWriteNext();
					if (chunkSection == null) {
						continue;
					}

					int blockCount = chunkSection.blockCount;
					Palette.Builder builder = BlockStrategy.createPaletteBuilder();

					final int baseY = heightAccessor.getMinBuildHeight() + (writer.getSectionIndex() << 4);
					for (int index = 0; index < 4096; index++) {
						int blockState = chunkSection.getBlockState(index);
	
						int y = baseY + (index >> 8 & 15);
						if (bundle.shouldProcessBlock(y)) {
	
							int obfuscateBits = blockFlags.flags(blockState, y);
							if (!BlockFlags.isEmpty(obfuscateBits)) {
	
								int x = baseX + (index & 15);
								int z = baseZ + (index >> 4 & 15);
								boolean obfuscated = true;
	
								if (BlockFlags.isObfuscateBitSet(obfuscateBits)
										&& shouldObfuscate(task, chunk, x, y, z)
										&& obfuscationConfig.shouldObfuscate(y)) {
									blockState = bundle.nextRandomObfuscationBlock(y);
								} else if (BlockFlags.isProximityBitSet(obfuscateBits)
										&& proximityConfig.shouldObfuscate(y)) {
									proximityBlocks.add(new BlockPos(x, y, z));
	
									if (BlockFlags.isUseBlockBelowBitSet(obfuscateBits)) {
										blockState = getBlockStateBelow(blockFlags, chunk, x, y, z);
										if (blockState == -1) {
											blockState = bundle.nextRandomProximityBlock(y);
										}
									} else {
										blockState = bundle.nextRandomProximityBlock(y);
									}
								} else {
									obfuscated = false;
								}
	
								if (obfuscated) {
									if (BlockFlags.isBlockEntityBitSet(obfuscateBits)) {
										blockEntities.add(new BlockPos(x, y, z));
									}
									if (OrebfuscatorNms.isAir(blockState)) {
										blockCount--;
									}
								}
							}
						}
	
						builder.add(blockState);
						blockStates[index] = blockState;
					}
	
					writer.writeSection(builder.build(), blockCount, blockStates, chunkSection.biomeSlice);
				} catch (Exception e) {
					throw new RuntimeException("error in section: " + writer.getSectionIndex(), e);
				}
			}

			long duration = System.currentTimeMillis() - time;
			int count = COUNT.incrementAndGet();
			if (count > 0) {
				double totalTime = (double) TIME.addAndGet(duration);
				if (count % 100 == 0) {
					Bukkit.broadcastMessage("ms/chunk: " + (totalTime / count));
				}
			}

			task.complete(chunk.finalizeOutput(), blockEntities, proximityBlocks);
		} catch (Exception e) {
			task.completeExceptionally(e);
		}
	}

	// returns first block below given position that wouldn't be obfuscated in any
	// way at given position
	private int getBlockStateBelow(BlockFlags blockFlags, Chunk chunk, int x, int y, int z) {
		for (int targetY = y - 1; targetY > chunk.getHeightAccessor().getMinBuildHeight(); targetY--) {
			int blockData = chunk.getBlockState(x, targetY, z);
			if (blockData != -1) {
				int mask = blockFlags.flags(blockData, y);
				if (BlockFlags.isEmpty(mask) || BlockFlags.isAllowForUseBlockBelowBitSet(mask)) {
					return blockData;
				}
			}
		}
		return -1;
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
