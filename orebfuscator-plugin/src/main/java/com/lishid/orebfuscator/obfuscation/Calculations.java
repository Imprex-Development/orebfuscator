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

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtType;
import com.lishid.orebfuscator.NmsInstance;
import com.lishid.orebfuscator.Orebfuscator;
import com.lishid.orebfuscator.chunkmap.ChunkData;
import com.lishid.orebfuscator.chunkmap.ChunkMapManager;
import com.lishid.orebfuscator.config.ProximityHiderConfig;
import com.lishid.orebfuscator.config.WorldConfig;
import com.lishid.orebfuscator.utils.Globals;

import net.imprex.orebfuscator.cache.ChunkCache;
import net.imprex.orebfuscator.cache.ChunkCacheEntry;
import net.imprex.orebfuscator.util.BlockCoords;
import net.imprex.orebfuscator.util.ChunkPosition;

public class Calculations {

	public static class Result {
		public byte[] output;
		public List<BlockCoords> removedEntities;
	}

	private static Random random = new Random();

	public static ChunkCacheEntry obfuscateChunk(ChunkData chunkData, Player player, WorldConfig worldConfig) {
		List<BlockCoords> proximityBlocks = new ArrayList<>();
		List<BlockCoords> removedEntities = new ArrayList<>();
		try {
			long hash = CalculationsUtil.Hash(chunkData.data, chunkData.data.length);
			byte[] data = obfuscate(worldConfig, chunkData, player, proximityBlocks, removedEntities);

			ChunkCacheEntry chunkCacheEntry = new ChunkCacheEntry(hash, data);
			chunkCacheEntry.getProximityBlocks().addAll(proximityBlocks);
			chunkCacheEntry.getRemovedEntities().addAll(removedEntities);
			return chunkCacheEntry;
		} catch (Exception e) {
			throw new RuntimeException("Can't obfuscate chunk " + chunkData.chunkX + ", " + chunkData.chunkZ);
		}
	}

	private static LinkedList<Long> avgTimes = new LinkedList<>();
	private static double calls = 0;
	private static DecimalFormat formatter = new DecimalFormat("###,###,###,###.00");

	public static Result obfuscateOrUseCache(ChunkData chunkData, Player player, WorldConfig worldConfig) {
		long time = System.nanoTime();
		Result result = obfuscateOrUseCache0(chunkData, player, worldConfig);
		long diff = System.nanoTime() - time;

		avgTimes.add(diff);
		if (avgTimes.size() > 1000) {
			avgTimes.removeFirst();
		}

		if (calls++ % 100 == 0) {
			System.out.println("avg: "
					+ formatter.format(
							((double) avgTimes.stream().reduce(0L, Long::sum) / (double) avgTimes.size()) / 1000D)
					+ "μs");
		}

		return result;
	}

	public static Result obfuscateOrUseCache0(ChunkData chunkData, Player player, WorldConfig worldConfig) {
		if (chunkData.primaryBitMask == 0) {
			return null;
		}

		ChunkPosition position = new ChunkPosition(player.getWorld().getName(), chunkData.chunkX, chunkData.chunkZ);
		ChunkCacheEntry cacheEntry = ChunkCache.get(position, key -> obfuscateChunk(chunkData, player, worldConfig));

		Result result = new Result();
		result.output = cacheEntry.getData();
		result.removedEntities = cacheEntry.getRemovedEntities();

		return result;
	}

	private static byte[] obfuscate(WorldConfig worldConfig, ChunkData chunkData, Player player,
			List<BlockCoords> proximityBlocks, List<BlockCoords> removedEntities) throws IOException {
		ProximityHiderConfig proximityHider = worldConfig.getProximityHiderConfig();
		int initialRadius = Orebfuscator.config.getInitialRadius();

		// Track of pseudo-randomly assigned randomBlock
		int randomIncrement = 0;
		int randomIncrement2 = 0;
		int randomCave = 0;

		int engineMode = Orebfuscator.config.getEngineMode();
		int maxChance = worldConfig.getAirGeneratorMaxChance();

		int randomBlocksLength = worldConfig.getRandomBlocks().length;
		boolean randomAlternate = false;

		int startX = chunkData.chunkX << 4;
		int startZ = chunkData.chunkZ << 4;

		byte[] output;

		try (ChunkMapManager manager = ChunkMapManager.create(chunkData)) {
			for (int i = 0; i < manager.getSectionCount(); i++) {
				worldConfig.shuffleRandomBlocks();

				for (int offsetY = 0; offsetY < 16; offsetY++) {
					for (int offsetZ = 0; offsetZ < 16; offsetZ++) {
						int incrementMax = (maxChance + random(maxChance)) / 2;

						for (int offsetX = 0; offsetX < 16; offsetX++) {
							int blockData = manager.readNextBlock();
							int x = startX | offsetX;
							int y = manager.getY();
							int z = startZ | offsetZ;

							// Initialize data
							int obfuscateBits = worldConfig.getObfuscatedBits(blockData);
							boolean obfuscateFlag = (obfuscateBits & Globals.MASK_OBFUSCATE) != 0;
							boolean proximityHiderFlag = (obfuscateBits & Globals.MASK_PROXIMITYHIDER) != 0;
							boolean darknessBlockFlag = (obfuscateBits & Globals.MASK_DARKNESSBLOCK) != 0;
							boolean tileEntityFlag = (obfuscateBits & Globals.MASK_TILEENTITY) != 0;

							boolean obfuscate = false;
							boolean specialObfuscate = false;

							// Check if the block should be obfuscated for the default engine modes
							if (obfuscateFlag) {
								if (initialRadius == 0) {
									// Do not interfere with PH
									if (proximityHiderFlag && proximityHider.isEnabled()
											&& proximityHider.isProximityObfuscated(y, blockData)) {
										if (!areAjacentBlocksTransparent(manager, player.getWorld(), false, x, y, z,
												1)) {
											obfuscate = true;
										}
									} else {
										// Obfuscate all blocks
										obfuscate = true;
									}
								} else {
									// Check if any nearby blocks are transparent
									if (!areAjacentBlocksTransparent(manager, player.getWorld(), false, x, y, z,
											initialRadius)) {
										obfuscate = true;
									}
								}
							}

							// Check if the block should be obfuscated because of proximity check
							if (!obfuscate && proximityHiderFlag && proximityHider.isEnabled()
									&& proximityHider.isProximityObfuscated(y, blockData)) {
								BlockCoords block = new BlockCoords(x, y, z);
								if (block != null) {
									proximityBlocks.add(block);
								}

								obfuscate = true;
								if (proximityHider.isUseSpecialBlock()) {
									specialObfuscate = true;
								}
							}

							// Check if the block is obfuscated
							if (obfuscate && (!worldConfig.isBypassObfuscationForSignsWithText()
									|| canObfuscate(chunkData, x, y, z, blockData))) {
								if (specialObfuscate) {
									// Proximity hider
									blockData = proximityHider.getSpecialBlockID();
								} else {
									if (engineMode == 1) {
										// Engine mode 1, replace with stone
										blockData = worldConfig.getMode1BlockId();
									} else if (engineMode == 2) {
										// Ending mode 2, replace with random block
										if (randomBlocksLength > 1) {
											randomIncrement = CalculationsUtil.increment(randomIncrement,
													randomBlocksLength);
										}

										blockData = worldConfig.getRandomBlock(randomIncrement, randomAlternate);
										randomAlternate = !randomAlternate;
									}
									// Anti texturepack and freecam
									if (worldConfig.isAntiTexturePackAndFreecam()) {
										// Add random air blocks
										randomIncrement2 = random(incrementMax);

										if (randomIncrement2 == 0) {
											randomCave = 1 + random(3);
										}

										if (randomCave > 0) {
											blockData = NmsInstance.current.getCaveAirBlockId();
											randomCave--;
										}
									}
								}
							}

							// Check if the block should be obfuscated because of the darkness
							if (!obfuscate && darknessBlockFlag && worldConfig.isDarknessHideBlocks()) {
								if (!areAjacentBlocksBright(player.getWorld(), x, y, z, 1)) {
									// Hide block, setting it to air
									blockData = NmsInstance.current.getCaveAirBlockId();
									obfuscate = true;
								}
							}

							if (obfuscate && tileEntityFlag) {
								removedEntities.add(new BlockCoords(x, y, z));
							}

							if (offsetY == 0 && offsetZ == 0 && offsetX == 0) {
								manager.finalizeOutput();
								manager.initOutputPalette();
								addBlocksToPalette(manager, worldConfig);
								manager.initOutputSection();
							}

							manager.writeOutputBlock(blockData);
						}
					}
				}
			}

			manager.finalizeOutput();

			output = manager.createOutput();
		}

		ProximityHider.addProximityBlocks(player, chunkData.chunkX, chunkData.chunkZ, proximityBlocks);

		// Orebfuscator.log("Create new chunk data for x = " + chunkData.chunkX + ", z =
		// " + chunkData.chunkZ);/*debug*/

		return output;
	}

	private static boolean canObfuscate(ChunkData chunkData, int x, int y, int z, int blockData) {
		if (!NmsInstance.current.isSign(blockData)) {
			return true;
		}

		NbtCompound tag = getBlockEntity(chunkData, x, y, z);

		return tag == null || isSignTextEmpty(tag, "Text1") && isSignTextEmpty(tag, "Text2")
				&& isSignTextEmpty(tag, "Text3") && isSignTextEmpty(tag, "Text4");
	}

	private static boolean isSignTextEmpty(NbtCompound compound, String key) {
		NbtBase<?> tag = compound.getValue(key);

		if (tag == null || tag.getType() != NbtType.TAG_STRING) {
			return true;
		}

		String json = (String) tag.getValue();

		if (json == null || json.isEmpty()) {
			return true;
		}

		String text = NmsInstance.current.getTextFromChatComponent(json);

		return text == null || text.isEmpty();
	}

	private static NbtCompound getBlockEntity(ChunkData chunkData, int x, int y, int z) {
		for (NbtCompound tag : chunkData.blockEntities) {
			if (tag != null && x == tag.getInteger("x") && y == tag.getInteger("y") && z == tag.getInteger("z")) {
				return tag;
			}
		}

		return null;
	}

	private static void addBlocksToPalette(ChunkMapManager manager, WorldConfig worldConfig) {
		if (!manager.inputHasNonAirBlock()) {
			return;
		}

		for (int id : worldConfig.getPaletteBlocks()) {
			manager.addToOutputPalette(id);
		}
	}

	public static boolean areAjacentBlocksTransparent(ChunkMapManager manager, World world, boolean checkCurrentBlock,
			int x, int y, int z, int countdown) throws IOException {
		if (y >= world.getMaxHeight() || y < 0) {
			return true;
		}

		if (checkCurrentBlock) {
			ChunkData chunkData = manager.getChunkData();
			int blockData = manager.get(x, y, z);

			if (blockData < 0) {
				blockData = NmsInstance.current.loadChunkAndGetBlockId(world, x, y, z);

				if (blockData < 0) {
					chunkData.useCache = false;
				}
			}

			if (blockData >= 0 && Orebfuscator.config.isBlockTransparent(blockData)) {
				return true;
			}
		}

		if (countdown == 0) {
			return false;
		}

		if (areAjacentBlocksTransparent(manager, world, true, x, y + 1, z, countdown - 1)) {
			return true;
		}
		if (areAjacentBlocksTransparent(manager, world, true, x, y - 1, z, countdown - 1)) {
			return true;
		}
		if (areAjacentBlocksTransparent(manager, world, true, x + 1, y, z, countdown - 1)) {
			return true;
		}
		if (areAjacentBlocksTransparent(manager, world, true, x - 1, y, z, countdown - 1)) {
			return true;
		}
		if (areAjacentBlocksTransparent(manager, world, true, x, y, z + 1, countdown - 1)) {
			return true;
		}
		if (areAjacentBlocksTransparent(manager, world, true, x, y, z - 1, countdown - 1)) {
			return true;
		}

		return false;
	}

	public static boolean areAjacentBlocksBright(World world, int x, int y, int z, int countdown) {
		if (NmsInstance.current.getBlockLightLevel(world, x, y, z) > 0) {
			return true;
		}

		if (countdown == 0) {
			return false;
		}

		if (areAjacentBlocksBright(world, x, y + 1, z, countdown - 1)) {
			return true;
		}
		if (areAjacentBlocksBright(world, x, y - 1, z, countdown - 1)) {
			return true;
		}
		if (areAjacentBlocksBright(world, x + 1, y, z, countdown - 1)) {
			return true;
		}
		if (areAjacentBlocksBright(world, x - 1, y, z, countdown - 1)) {
			return true;
		}
		if (areAjacentBlocksBright(world, x, y, z + 1, countdown - 1)) {
			return true;
		}
		if (areAjacentBlocksBright(world, x, y, z - 1, countdown - 1)) {
			return true;
		}

		return false;
	}

	private static int random(int max) {
		return random.nextInt(max);
	}
}