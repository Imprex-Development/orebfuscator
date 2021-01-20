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
import net.imprex.orebfuscator.util.BlockPos;
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

		deobfuscate(Arrays.asList(block), true);
	}

	public void deobfuscate(Collection<? extends Block> blocks, boolean occluding) {
		if (blocks.isEmpty()) {
			return;
		}

		World world = Iterables.get(blocks, 0).getWorld();
		WorldConfig worldConfig = this.config.world(world);
		if (worldConfig == null || !worldConfig.enabled()) {
			return;
		}

		int updateRadius = this.config.general().updateRadius();
		BlockMask blockMask = this.config.blockMask(world);

		Processor processor = new Processor(blockMask);
		for (Block block : blocks) {
			if (!occluding || block.getType().isOccluding()) {
				BlockStateHolder blockState = NmsInstance.getBlockState(world, block);
				processor.updateAdjacentBlocks(blockState, updateRadius);
			}
		}
	}

	public class Processor {

		private final Set<BlockPos> updatedBlocks = new HashSet<>();
		private final Set<ChunkPosition> invalidChunks = new HashSet<>();

		private final BlockMask blockMask;

		public Processor(BlockMask blockMask) {
			this.blockMask = blockMask;
		}

		public void updateAdjacentBlocks(BlockStateHolder blockState, int depth) {
			if (blockState == null) {
				return;
			}
			
			World world = blockState.getWorld();
			BlockPos position = blockState.getPosition();

			int blockId = blockState.getBlockId();
			if (BlockMask.isObfuscateBitSet(blockMask.mask(blockId)) && updatedBlocks.add(position)) {
				blockState.notifyBlockChange();

				if (config.cache().enabled()) {

					ChunkPosition chunkPosition = position.toChunkPosition(world);
					if (this.invalidChunks.add(chunkPosition)) {
						chunkCache.invalidate(chunkPosition);
					}
				}
			}

			if (depth-- > 0) {
				int x = blockState.getX();
				int y = blockState.getY();
				int z = blockState.getZ();

				updateAdjacentBlocks(NmsInstance.getBlockState(world, x + 1, y, z), depth);
				updateAdjacentBlocks(NmsInstance.getBlockState(world, x - 1, y, z), depth);
				updateAdjacentBlocks(NmsInstance.getBlockState(world, x, y + 1, z), depth);
				updateAdjacentBlocks(NmsInstance.getBlockState(world, x, y - 1, z), depth);
				updateAdjacentBlocks(NmsInstance.getBlockState(world, x, y, z + 1), depth);
				updateAdjacentBlocks(NmsInstance.getBlockState(world, x, y, z - 1), depth);
			}
		}
	}
}
