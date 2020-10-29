package net.imprex.orebfuscator.obfuscation;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.World;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.cache.ChunkCacheRequest;
import net.imprex.orebfuscator.chunk.Chunk;
import net.imprex.orebfuscator.chunk.ChunkSection;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.BlockMask;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.ProximityConfig;
import net.imprex.orebfuscator.config.WorldConfig;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.MaterialUtil;

public class Obfuscator {

	private final Orebfuscator orebfuscator;
	private final OrebfuscatorConfig config;

	public Obfuscator(Orebfuscator orebfuscator) {
		this.orebfuscator = orebfuscator;
		this.config = orebfuscator.getOrebfuscatorConfig();
	}

	public CompletableFuture<ObfuscatedChunk> obfuscate(ChunkCacheRequest request) {
		CompletableFuture<ObfuscatedChunk> future = new CompletableFuture<>();
		if (this.orebfuscator.isMainThread()) {
			future.complete(this.obfuscateNow(request));
		} else {
			Bukkit.getScheduler().runTask(this.orebfuscator, () -> {
				future.complete(this.obfuscateNow(request));
			});
		}
		return future;
	}

	private ObfuscatedChunk obfuscateNow(ChunkCacheRequest request) {
		World world = request.getKey().getWorld();
		byte[] hash = request.getHash();
		ChunkStruct chunkStruct = request.getChunkStruct();

		BlockMask blockMask = this.config.blockMask(world);
		WorldConfig worldConfig = this.config.world(world);
		ProximityConfig proximityConfig = this.config.proximity(world);
		int initialRadius = this.config.general().initialRadius();

		Set<BlockPos> proximityBlocks = new HashSet<>();
		Set<BlockPos> removedTileEntities = new HashSet<>();

		int baseX = chunkStruct.chunkX << 4;
		int baseZ = chunkStruct.chunkZ << 4;

		try (Chunk chunk = Chunk.fromChunkStruct(chunkStruct)) {
			for (int sectionIndex = 0; sectionIndex < chunk.getSectionCount(); sectionIndex++) {
				ChunkSection chunkSection = chunk.getSection(sectionIndex);
				// TODO faster buffer + pre calc palette

				// skip empty chunkSections
				if (chunkSection == null) {
					continue;
				}

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
					boolean tileEntityFlag = (obfuscateBits & BlockMask.BLOCK_MASK_TILEENTITY) != 0;
					boolean proximityFlag = (obfuscateBits & BlockMask.BLOCK_MASK_PROXIMITY) != 0;

					boolean obfuscate = false;

					// Check if the block should be obfuscated
					if (obfuscateFlag && !areAjacentBlocksTransparent(chunk, world, x, y, z, false, initialRadius)) {
						obfuscate = true;
					}

					// Check if the block should be obfuscated because of proximity check
					if (!obfuscate && proximityFlag) {
						proximityBlocks.add(new BlockPos(x, y, z));
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

					// remove obfuscated tile entities
					if (obfuscate && tileEntityFlag) {
						removedTileEntities.add(new BlockPos(x, y, z));
					}
				}
			}

			byte[] data = chunk.finalizeOutput();

			return new ObfuscatedChunk(hash, data, proximityBlocks, removedTileEntities);
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
				blockId = NmsInstance.loadChunkAndGetBlockId(world, x, y, z);
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
}
