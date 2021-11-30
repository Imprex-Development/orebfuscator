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
import net.imprex.orebfuscator.config.BlockFlags;
import net.imprex.orebfuscator.config.ObfuscationConfig;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.ProximityConfig;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.HeightAccessor;

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
		HeightAccessor heightAccessor = HeightAccessor.get(world);

		BlockFlags blockFlags = this.config.blockFlags(world);
		ObfuscationConfig obfuscationConfig = this.config.obfuscation(world);
		ProximityConfig proximityConfig = this.config.proximity(world);
		int initialRadius = this.config.general().initialRadius();

		Set<BlockPos> proximityBlocks = new HashSet<>();
		Set<BlockPos> removedTileEntities = new HashSet<>();

		int baseX = chunkStruct.chunkX << 4;
		int baseZ = chunkStruct.chunkZ << 4;

		try (Chunk chunk = Chunk.fromChunkStruct(chunkStruct)) {
			for (int sectionIndex = 0; sectionIndex < chunk.getSectionCount(); sectionIndex++) {
				ChunkSection chunkSection = chunk.getSection(sectionIndex);
				if (chunkSection == null) {
					continue;
				}

				final int baseY = heightAccessor.getMinBuildHeight() + (sectionIndex << 4);
				for (int index = 0; index < 4096; index++) {
					int blockData = chunkSection.getBlock(index);

					int y = baseY + (index >> 8 & 15);

					int obfuscateBits = blockFlags.flags(blockData, y);
					if (BlockFlags.isEmpty(obfuscateBits)) {
						continue;
					}

					int x = baseX + (index & 15);
					int z = baseZ + (index >> 4 & 15);

					boolean obfuscated = false;

					// should current block be obfuscated
					if (BlockFlags.isObfuscateBitSet(obfuscateBits)
							&& shouldObfuscate(chunk, world, x, y, z, initialRadius)) {
						blockData = obfuscationConfig.nextRandomBlockId();
						obfuscated = true;
					}

					// should current block be proximity hidden
					if (!obfuscated && BlockFlags.isProximityBitSet(obfuscateBits)) {
						proximityBlocks.add(new BlockPos(x, y, z));
						obfuscated = true;
						if (BlockFlags.isUseBlockBelowBitSet(obfuscateBits)) {
							blockData = getBlockBelow(blockFlags, chunk, x, y, z);
						} else {
							blockData = proximityConfig.nextRandomBlockId();
						}
					}

					// update block state if needed
					if (obfuscated) {
						chunkSection.setBlock(index, blockData);
						if (BlockFlags.isTileEntityBitSet(obfuscateBits)) {
							removedTileEntities.add(new BlockPos(x, y, z));
						}
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

	// returns first block below given position that wouldn't be obfuscated in any way at given position
	private int getBlockBelow(BlockFlags blockFlags, Chunk chunk, int x, int y, int z) {
		for (int targetY = y - 1; targetY > chunk.getHeightAccessor().getMinBuildHeight(); targetY--) {
			int blockData = chunk.getBlock(x, targetY, z);
			if (blockData != -1 && BlockFlags.isEmpty(blockFlags.flags(blockData, y))) {
				return blockData;
			}
		}
		return 0;
	}

	private boolean shouldObfuscate(Chunk chunk, World world, int x, int y, int z, int depth) {
		return !areAjacentBlocksTransparent(chunk, world, x, y, z, false, depth);
	}

	private boolean areAjacentBlocksTransparent(Chunk chunk, World world, int x, int y, int z, boolean check,
			int depth) {
		if (y >= world.getMaxHeight() || y < chunk.getHeightAccessor().getMinBuildHeight()) {
			return true;
		}

		if (check) {
			int blockId = chunk.getBlock(x, y, z);
			if (blockId == -1) {
				blockId = NmsInstance.loadChunkAndGetBlockId(world, x, y, z);
			}
			if (blockId >= 0 && !NmsInstance.isOccluding(blockId)) {
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
