/*
 * Copyright (C) 2011-2014 lishid.  All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation,  version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.lishid.orebfuscator.obfuscation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.lishid.orebfuscator.Orebfuscator;
import com.lishid.orebfuscator.config.ConfigManager;
import com.lishid.orebfuscator.config.WorldConfig;
import com.lishid.orebfuscator.nms.IBlockInfo;
import com.lishid.orebfuscator.types.ChunkCoord;
import com.lishid.orebfuscator.utils.Globals;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.cache.ChunkCache;
import net.imprex.orebfuscator.util.ChunkPosition;

public class BlockUpdate {

	private static ConfigManager configManager;
	private static ChunkCache chunkCache;

	public static void initialize(Orebfuscator orebfuscator) {
		BlockUpdate.configManager = orebfuscator.getConfigManager();
		BlockUpdate.chunkCache = orebfuscator.getChunkCache();
	}

	public static boolean needsUpdate(Block block) {
		int materialId = NmsInstance.get().getMaterialIds(block.getType()).iterator().next();
		return !BlockUpdate.configManager.getConfig().isBlockTransparent(materialId);
	}

	public static void update(Block block) {
		if (!needsUpdate(block)) {
			return;
		}

		update(Arrays.asList(new Block[] { block }));
	}

	public static void update(List<Block> blocks) {
		if (blocks.isEmpty()) {
			return;
		}

		World world = blocks.get(0).getWorld();
		WorldConfig worldConfig = BlockUpdate.configManager.getWorld(world);
		HashSet<IBlockInfo> updateBlocks = new HashSet<>();
		HashSet<ChunkCoord> invalidChunks = new HashSet<>();
		int updateRadius = BlockUpdate.configManager.getConfig().getUpdateRadius();

		for (Block block : blocks) {
			if (needsUpdate(block)) {
				IBlockInfo blockInfo = NmsInstance.get().getBlockInfo(world, block.getX(), block.getY(),
						block.getZ());

				getAdjacentBlocks(updateBlocks, world, worldConfig, blockInfo, updateRadius);

				if (blockInfo != null) {
					if ((blockInfo.getX() & 0xf) == 0) {
						invalidChunks.add(new ChunkCoord((blockInfo.getX() >> 4) - 1, blockInfo.getZ() >> 4));
					} else if ((blockInfo.getX() + 1 & 0xf) == 0) {
						invalidChunks.add(new ChunkCoord((blockInfo.getX() >> 4) + 1, blockInfo.getZ() >> 4));
					} else if ((blockInfo.getZ() & 0xf) == 0) {
						invalidChunks.add(new ChunkCoord(blockInfo.getX() >> 4, (blockInfo.getZ() >> 4) - 1));
					} else if ((blockInfo.getZ() + 1 & 0xf) == 0) {
						invalidChunks.add(new ChunkCoord(blockInfo.getX() >> 4, (blockInfo.getZ() >> 4) + 1));
					}
				}
			}
		}

		sendUpdates(world, updateBlocks);

		invalidateCachedChunks(world, invalidChunks);
	}

	// This method is used in CastleGates plugin
	public static void updateByLocations(List<Location> locations, int updateRadius) {
		if (locations.isEmpty()) {
			return;
		}

		World world = locations.get(0).getWorld();
		WorldConfig worldConfig = BlockUpdate.configManager.getWorld(world);
		HashSet<IBlockInfo> updateBlocks = new HashSet<>();
		HashSet<ChunkCoord> invalidChunks = new HashSet<>();

		for (Location location : locations) {
			IBlockInfo blockInfo = NmsInstance.get().getBlockInfo(world, location.getBlockX(), location.getBlockY(),
					location.getBlockZ());

			getAdjacentBlocks(updateBlocks, world, worldConfig, blockInfo, updateRadius);

			if (blockInfo != null) {
				if ((blockInfo.getX() & 0xf) == 0) {
					invalidChunks.add(new ChunkCoord((blockInfo.getX() >> 4) - 1, blockInfo.getZ() >> 4));
				} else if ((blockInfo.getX() + 1 & 0xf) == 0) {
					invalidChunks.add(new ChunkCoord((blockInfo.getX() >> 4) + 1, blockInfo.getZ() >> 4));
				} else if ((blockInfo.getZ() & 0xf) == 0) {
					invalidChunks.add(new ChunkCoord(blockInfo.getX() >> 4, (blockInfo.getZ() >> 4) - 1));
				} else if ((blockInfo.getZ() + 1 & 0xf) == 0) {
					invalidChunks.add(new ChunkCoord(blockInfo.getX() >> 4, (blockInfo.getZ() >> 4) + 1));
				}
			}
		}

		sendUpdates(world, updateBlocks);

		invalidateCachedChunks(world, invalidChunks);
	}

	private static void sendUpdates(World world, Set<IBlockInfo> blocks) {
		// Orebfuscator.log("Notify block change for " + blocks.size() + "
		// blocks");/*debug*/

		for (IBlockInfo blockInfo : blocks) {
			NmsInstance.get().notifyBlockChange(world, blockInfo);
		}
	}

	private static void invalidateCachedChunks(World world, Set<ChunkCoord> invalidChunks) {
		if (invalidChunks.isEmpty() || !BlockUpdate.configManager.getConfig().isUseCache()) {
			return;
		}

		for (ChunkCoord chunk : invalidChunks) {
			BlockUpdate.chunkCache.invalidate(new ChunkPosition(world.getName(), chunk.x, chunk.z));
		}
	}

	private static void getAdjacentBlocks(HashSet<IBlockInfo> allBlocks, World world, WorldConfig worldConfig,
			IBlockInfo blockInfo, int countdown) {
		if (blockInfo == null) {
			return;
		}

		int blockId = blockInfo.getCombinedId();

		if ((worldConfig.getObfuscatedBits(blockId) & Globals.MASK_OBFUSCATE) != 0) {
			allBlocks.add(blockInfo);
		}

		if (countdown > 0) {
			countdown--;
			getAdjacentBlocks(allBlocks, world, worldConfig,
					NmsInstance.get().getBlockInfo(world, blockInfo.getX() + 1, blockInfo.getY(), blockInfo.getZ()),
					countdown);
			getAdjacentBlocks(allBlocks, world, worldConfig,
					NmsInstance.get().getBlockInfo(world, blockInfo.getX() - 1, blockInfo.getY(), blockInfo.getZ()),
					countdown);
			getAdjacentBlocks(allBlocks, world, worldConfig,
					NmsInstance.get().getBlockInfo(world, blockInfo.getX(), blockInfo.getY() + 1, blockInfo.getZ()),
					countdown);
			getAdjacentBlocks(allBlocks, world, worldConfig,
					NmsInstance.get().getBlockInfo(world, blockInfo.getX(), blockInfo.getY() - 1, blockInfo.getZ()),
					countdown);
			getAdjacentBlocks(allBlocks, world, worldConfig,
					NmsInstance.get().getBlockInfo(world, blockInfo.getX(), blockInfo.getY(), blockInfo.getZ() + 1),
					countdown);
			getAdjacentBlocks(allBlocks, world, worldConfig,
					NmsInstance.get().getBlockInfo(world, blockInfo.getX(), blockInfo.getY(), blockInfo.getZ() - 1),
					countdown);
		}
	}
}
