package net.imprex.orebfuscator.obfuscation;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.World;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.cache.ChunkCache;
import net.imprex.orebfuscator.cache.ChunkCacheEntry;
import net.imprex.orebfuscator.chunk.Chunk;
import net.imprex.orebfuscator.chunk.ChunkSection;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.BlockMask;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.ProximityConfig;
import net.imprex.orebfuscator.config.WorldConfig;
import net.imprex.orebfuscator.util.BlockCoords;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.util.MaterialUtil;

public class Obfuscator {

	private final OrebfuscatorConfig config;
	private final ChunkCache chunkCache;

	public Obfuscator(Orebfuscator orebfuscator) {
		this.config = orebfuscator.getOrebfuscatorConfig();
		this.chunkCache = orebfuscator.getChunkCache();
	}

	public ChunkCacheEntry obfuscateOrUseCache(World world, ChunkStruct chunkStruct) {
		if (chunkStruct.primaryBitMask == 0) {
			return null;
		}

		final ChunkPosition position = new ChunkPosition(world.getName(), chunkStruct.chunkX, chunkStruct.chunkZ);
		final byte[] hash = ChunkCache.hash(this.config.hash(), chunkStruct.data);

		if (this.config.cache().enabled()) {
			return this.chunkCache.get(position, hash, key -> this.obfuscate(hash, world, chunkStruct));
		} else {
			return this.obfuscate(hash, world, chunkStruct);
		}
	}

	private ChunkCacheEntry obfuscate(byte[] hash, World world, ChunkStruct chunkStruct) {
		BlockMask blockMask = this.config.blockMask(world);
		WorldConfig worldConfig = this.config.world(world);
		ProximityConfig proximityConfig = this.config.proximity(world);
		int initialRadius = this.config.general().initialRadius();

		Set<BlockCoords> proximityBlocks = new HashSet<>();
		Set<BlockCoords> removedTileEntities = new HashSet<>();

		int baseX = chunkStruct.chunkX << 4;
		int baseZ = chunkStruct.chunkZ << 4;

		try (Chunk chunk = Chunk.fromChunkStruct(chunkStruct)) {
			for (int sectionIndex = 0; sectionIndex < chunk.getSectionCount(); sectionIndex++) {
				ChunkSection chunkSection = chunk.getSection(sectionIndex);
				// TODO faster buffer + pre calc palette

				final int baseY = sectionIndex * 16;
				for (int index = 0; index < 4096; index++) {
					int blockData = chunkSection.getBlock(index);

					int y = baseY + (index >> 8 & 15);

					int obfuscateBits = blockMask.mask(blockData, y);
					if ((obfuscateBits & 0xF) == 0) {
						continue;
					}

					int x = baseX + (index & 15);
					int z = baseZ + (index >> 4 & 15);

					boolean obfuscateFlag = (obfuscateBits & BlockMask.BLOCK_MASK_OBFUSCATE) != 0;
					boolean darknessFlag = (obfuscateBits & BlockMask.BLOCK_MASK_DARKNESS) != 0;
					boolean tileEntityFlag = (obfuscateBits & BlockMask.BLOCK_MASK_TILEENTITY) != 0;
					boolean proximityFlag = (obfuscateBits & BlockMask.BLOCK_MASK_PROXIMITY) != 0;

					boolean obfuscate = false;

					// Check if the block should be obfuscated
					if (obfuscateFlag && !areAjacentBlocksTransparent(chunk, world, x, y, z, false, initialRadius)) {
						obfuscate = true;
					}

					// Check if the block should be obfuscated because of proximity check
					if (!obfuscate && proximityFlag) {
						proximityBlocks.add(new BlockCoords(x, y, z));
						obfuscate = true;
					}

					// Check if the block is obfuscated
					if (obfuscate) {
						if (proximityFlag) {
							blockData = proximityConfig.randomBlockId();
						} else {
							blockData = worldConfig.randomBlockId();
						}
						chunkSection.setBlock(index, blockData);
					}

					// Check if the block should be obfuscated because of the darkness
					if (!obfuscate && darknessFlag && worldConfig.darknessBlocksEnabled()
							&& !areAjacentBlocksBright(world, x, y, z, 1)) {
						chunkSection.setBlock(index, NmsInstance.get().getCaveAirBlockId());
						obfuscate = true;
					}

					// remove obfuscated tile entities
					if (obfuscate && tileEntityFlag) {
						removedTileEntities.add(new BlockCoords(x, y, z));
					}
				}
			}

			byte[] data = chunk.finalizeOutput();

			return new ChunkCacheEntry(hash, data, proximityBlocks, removedTileEntities);
		} catch (Exception e) {
			e.printStackTrace();
			throw new Error(e);
		}
	}

	private static boolean areAjacentBlocksTransparent(Chunk chunk, World world, int x, int y, int z, boolean check,
			int depth) {
		if (y >= world.getMaxHeight() || y < 0) {
			return true;
		}

		if (check) {
			int blockId = chunk.getBlock(x, y, z);
			if (blockId == -1) {
				blockId = NmsInstance.get().loadChunkAndGetBlockId(world, x, y, z);
			}
			if (blockId >= 0 && MaterialUtil.isTransparent(blockId)) {
				return true;
			}
		}

		if (depth-- > 0) {
			return areAjacentBlocksTransparent(chunk, world, x, y + 1, z, true, depth)
					|| areAjacentBlocksTransparent(chunk, world, x, y - 1, z, true, depth)
					|| areAjacentBlocksTransparent(chunk, world, x + 1, y, z, true, depth)
					|| areAjacentBlocksTransparent(chunk, world, x - 1, y, z, true, depth)
					|| areAjacentBlocksTransparent(chunk, world, x, y, z + 1, true, depth)
					|| areAjacentBlocksTransparent(chunk, world, x, y, z - 1, true, depth);
		}

		return false;
	}

	public static boolean areAjacentBlocksBright(World world, int x, int y, int z, int depth) {
		if (NmsInstance.get().getBlockLightLevel(world, x, y, z) > 0) {
			return true;
		}

		if (depth-- > 0) {
			return areAjacentBlocksBright(world, x, y + 1, z, depth)
					|| areAjacentBlocksBright(world, x, y - 1, z, depth)
					|| areAjacentBlocksBright(world, x + 1, y, z, depth)
					|| areAjacentBlocksBright(world, x - 1, y, z, depth)
					|| areAjacentBlocksBright(world, x, y, z + 1, depth)
					|| areAjacentBlocksBright(world, x, y, z - 1, depth);
		}

		return false;
	}
}
