package net.imprex.orebfuscator.obfuscation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.block.Block;

import com.google.common.collect.Iterables;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.cache.ChunkCache;
import net.imprex.orebfuscator.config.BlockMask;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.WorldConfig;
import net.imprex.orebfuscator.nms.BlockStateHolder;
import net.imprex.orebfuscator.util.ChunkPosition;

public class Deobfuscator {

	private final OrebfuscatorConfig config;
	private final ChunkCache chunkCache;

	public Deobfuscator(Orebfuscator orebfuscator) {
		this.config = orebfuscator.getOrebfuscatorConfig();
		this.chunkCache = orebfuscator.getChunkCache();
	}

	void deobfuscate(Block block) {
		if (block == null || !block.getType().isOccluding()) {
			return;
		}

		deobfuscate(Arrays.asList(block));
	}

	public void deobfuscate(Collection<? extends Block> blocks) {
		if (blocks.isEmpty()) {
			return;
		}

		World world = Iterables.get(blocks, 0).getWorld();
		WorldConfig worldConfig = this.config.world(world);
		if (worldConfig == null || !worldConfig.enabled()) {
			return;
		}

		BlockMask blockMask = this.config.blockMask(world);

		Set<BlockStateHolder> updateBlocks = new HashSet<>();
		Set<ChunkPosition> invalidChunks = new HashSet<>();
		int updateRadius = this.config.general().updateRadius();

		for (Block block : blocks) {
			if (block.getType().isOccluding()) {
				int x = block.getX();
				int y = block.getY();
				int z = block.getZ();

				BlockStateHolder blockState = NmsInstance.getBlockState(world, x, y, z);
				if (blockState != null) {
					getAdjacentBlocks(updateBlocks, world, blockMask, blockState, updateRadius);
				}
			}
		}

		for (BlockStateHolder blockState : updateBlocks) {
			blockState.notifyBlockChange();
			invalidChunks.add(new ChunkPosition(world, blockState.getX() >> 4, blockState.getZ() >> 4));
		}

		if (!invalidChunks.isEmpty() && config.cache().enabled()) {
			for (ChunkPosition chunk : invalidChunks) {
				chunkCache.invalidate(chunk);
			}
		}
	}

	private void getAdjacentBlocks(Set<BlockStateHolder> updateBlocks, World world, BlockMask blockMask,
			BlockStateHolder blockState, int depth) {
		if (blockState == null) {
			return;
		}

		int blockId = blockState.getBlockId();
		if (BlockMask.isObfuscateSet(blockMask.mask(blockId))) {
			updateBlocks.add(blockState);
		}

		if (depth-- > 0) {
			int x = blockState.getX();
			int y = blockState.getY();
			int z = blockState.getZ();

			getAdjacentBlocks(updateBlocks, world, blockMask, NmsInstance.getBlockState(world, x + 1, y, z), depth);
			getAdjacentBlocks(updateBlocks, world, blockMask, NmsInstance.getBlockState(world, x - 1, y, z), depth);
			getAdjacentBlocks(updateBlocks, world, blockMask, NmsInstance.getBlockState(world, x, y + 1, z), depth);
			getAdjacentBlocks(updateBlocks, world, blockMask, NmsInstance.getBlockState(world, x, y - 1, z), depth);
			getAdjacentBlocks(updateBlocks, world, blockMask, NmsInstance.getBlockState(world, x, y, z + 1), depth);
			getAdjacentBlocks(updateBlocks, world, blockMask, NmsInstance.getBlockState(world, x, y, z - 1), depth);
		}
	}
}
