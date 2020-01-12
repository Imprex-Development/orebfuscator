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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.lishid.orebfuscator.Orebfuscator;
import com.lishid.orebfuscator.config.ConfigManager;
import com.lishid.orebfuscator.config.ProximityHiderConfig;
import com.lishid.orebfuscator.config.WorldConfig;
import com.lishid.orebfuscator.nms.IBlockInfo;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.util.BlockCoords;

public class ProximityHider extends Thread {

	private static final Map<Player, ProximityHiderPlayer> proximityHiderTracker = new HashMap<>();
	private static final Map<Player, Location> playersToCheck = new HashMap<>();
	private static final HashSet<Player> playersToReload = new HashSet<>();

	private static ProximityHider thread = new ProximityHider();

	private Map<Player, ProximityHiderPlayer> proximityHiderTrackerLocal = new HashMap<>();
	private long lastExecute = System.currentTimeMillis();
	private AtomicBoolean kill = new AtomicBoolean(false);
	private static boolean running = false;

	private static Orebfuscator orebfuscator;
	private static ConfigManager configManager;

	public static void initialize(Orebfuscator orebfuscator) {
		ProximityHider.orebfuscator = orebfuscator;
		ProximityHider.configManager = orebfuscator.getConfigManager();
	}

	public static void load() {
		running = true;
		if (thread == null || thread.isInterrupted() || !thread.isAlive()) {
			thread = new ProximityHider();
			thread.setName("Orebfuscator ProximityHider Thread");
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}
	}

	public static void terminate() {
		if (thread != null) {
			thread.kill.set(true);
		}
	}

	@Override
	public void run() {
		while (!this.isInterrupted() && !this.kill.get()) {
			try {
				// Wait until necessary
				long timeWait = this.lastExecute + ProximityHider.configManager.getConfig().getProximityHiderRate()
						- System.currentTimeMillis();
				this.lastExecute = System.currentTimeMillis();
				if (timeWait > 0) {
					Thread.sleep(timeWait);
				}

				if (!ProximityHider.configManager.getConfig().isProximityHiderEnabled()) {
					running = false;
					return;
				}

				HashMap<Player, Location> checkPlayers = new HashMap<>();

				synchronized (playersToCheck) {
					checkPlayers.putAll(playersToCheck);
					playersToCheck.clear();
				}

				for (Player p : checkPlayers.keySet()) {

					if (p == null) {
						continue;
					}

					synchronized (proximityHiderTracker) {
						if (!proximityHiderTracker.containsKey(p)) {
							continue;
						}
					}

					Location oldLocation = checkPlayers.get(p);

					if (oldLocation != null) {
						Location curLocation = p.getLocation();

						// Player didn't actually move
						if (curLocation.getBlockX() == oldLocation.getBlockX()
								&& curLocation.getBlockY() == oldLocation.getBlockY()
								&& curLocation.getBlockZ() == oldLocation.getBlockZ()) {
							continue;
						}
					}

					ProximityHiderPlayer localPlayerInfo = this.proximityHiderTrackerLocal.get(p);

					if (localPlayerInfo == null) {
						this.proximityHiderTrackerLocal.put(p,
								localPlayerInfo = new ProximityHiderPlayer(p.getWorld()));
					}

					synchronized (proximityHiderTracker) {
						ProximityHiderPlayer playerInfo = proximityHiderTracker.get(p);

						if (playerInfo != null) {
							if (!localPlayerInfo.getWorld().equals(playerInfo.getWorld())) {
								localPlayerInfo.setWorld(playerInfo.getWorld());
								localPlayerInfo.clearChunks();
							}

							localPlayerInfo.copyChunks(playerInfo);
							playerInfo.clearChunks();
						}
					}

					if (localPlayerInfo.getWorld() == null || p.getWorld() == null
							|| !p.getWorld().equals(localPlayerInfo.getWorld())) {
						localPlayerInfo.clearChunks();
						continue;
					}

					WorldConfig worldConfig = ProximityHider.configManager.getWorld(p.getWorld());
					ProximityHiderConfig proximityHider = worldConfig.getProximityHiderConfig();

					int checkRadius = proximityHider.getDistance() >> 4;

					if ((proximityHider.getDistance() & 0xf) != 0) {
						checkRadius++;
					}

					int distanceSquared = proximityHider.getDistanceSquared();

					ArrayList<BlockCoords> removedBlocks = new ArrayList<>();
					Location playerLocation = p.getLocation();
					// 4.3.1 -- GAZE CHECK
					Location playerEyes = p.getEyeLocation();
					// 4.3.1 -- GAZE CHECK END
					int minChunkX = (playerLocation.getBlockX() >> 4) - checkRadius;
					int maxChunkX = minChunkX + (checkRadius << 1);
					int minChunkZ = (playerLocation.getBlockZ() >> 4) - checkRadius;
					int maxChunkZ = minChunkZ + (checkRadius << 1);

					for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
						for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
							List<BlockCoords> blocks = localPlayerInfo.getBlocks(chunkX, chunkZ);

							if (blocks == null) {
								continue;
							}

							removedBlocks.clear();

							for (BlockCoords b : blocks) {
								if (b == null) {
									removedBlocks.add(b);
									continue;
								}

								Location blockLocation = new Location(localPlayerInfo.getWorld(), b.x, b.y, b.z);

								if (proximityHider.isObfuscateAboveY()
										|| playerLocation.distanceSquared(blockLocation) < distanceSquared) {
									// 4.3.1 -- GAZE CHECK
									if (!proximityHider.isUseFastGazeCheck() || this.doFastCheck(blockLocation,
											playerEyes, localPlayerInfo.getWorld())) {
										// 4.3.1 -- GAZE CHECK END
										removedBlocks.add(b);

										if (NmsInstance.get().sendBlockChange(p, blockLocation)) {
											final BlockCoords block = b;
											final Player player = p;
											ProximityHider.orebfuscator.runTask(new Runnable() {
												@Override
												public void run() {
													NmsInstance.get().updateBlockTileEntity(block, player);
												}
											});
										}
									}
								}
							}

							if (blocks.size() == removedBlocks.size()) {
								localPlayerInfo.removeChunk(chunkX, chunkZ);
							} else {
								blocks.removeAll(removedBlocks);
							}
						}
					}
				}
			} catch (Exception e) {
				Orebfuscator.log(e);
			}
		}

		running = false;
	}

	/**
	 * Basic idea here is to take some rays from the considered block to the
	 * player's eyes, and decide if any of those rays can reach the eyes unimpeded.
	 *
	 * @param block  the starting block
	 * @param eyes   the destination eyes
	 * @param player the player world we are testing for
	 * @return true if unimpeded path, false otherwise
	 */
	private boolean doFastCheck(Location block, Location eyes, World player) {
		double ex = eyes.getX();
		double ey = eyes.getY();
		double ez = eyes.getZ();
		double x = block.getBlockX();
		double y = block.getBlockY();
		double z = block.getBlockZ();
		return // midfaces
		this.fastAABBRayCheck(x, y, z, x, y + 0.5, z + 0.5, ex, ey, ez, player)
				|| this.fastAABBRayCheck(x, y, z, x + 0.5, y, z + 0.5, ex, ey, ez, player)
				|| this.fastAABBRayCheck(x, y, z, x + 0.5, y + 0.5, z, ex, ey, ez, player)
				|| this.fastAABBRayCheck(x, y, z, x + 0.5, y + 1.0, z + 0.5, ex, ey, ez, player)
				|| this.fastAABBRayCheck(x, y, z, x + 0.5, y + 0.5, z + 1.0, ex, ey, ez, player)
				|| this.fastAABBRayCheck(x, y, z, x + 1.0, y + 0.5, z + 0.5, ex, ey, ez, player) ||
				// corners
				this.fastAABBRayCheck(x, y, z, x, y, z, ex, ey, ez, player)
				|| this.fastAABBRayCheck(x, y, z, x + 1, y, z, ex, ey, ez, player)
				|| this.fastAABBRayCheck(x, y, z, x, y + 1, z, ex, ey, ez, player)
				|| this.fastAABBRayCheck(x, y, z, x + 1, y + 1, z, ex, ey, ez, player)
				|| this.fastAABBRayCheck(x, y, z, x, y, z + 1, ex, ey, ez, player)
				|| this.fastAABBRayCheck(x, y, z, x + 1, y, z + 1, ex, ey, ez, player)
				|| this.fastAABBRayCheck(x, y, z, x, y + 1, z + 1, ex, ey, ez, player)
				|| this.fastAABBRayCheck(x, y, z, x + 1, y + 1, z + 1, ex, ey, ez, player);
	}

	private boolean fastAABBRayCheck(double bx, double by, double bz, double x, double y, double z, double ex,
			double ey, double ez, World world) {
		double fx = ex - x;
		double fy = ey - y;
		double fz = ez - z;
		double absFx = Math.abs(fx);
		double absFy = Math.abs(fy);
		double absFz = Math.abs(fz);
		double s = Math.max(absFx, Math.max(absFy, absFz));

		if (s < 1) {
			return true; // on top / inside
		}

		double lx, ly, lz;

		fx = fx / s; // units of change along vector
		fy = fy / s;
		fz = fz / s;

		while (s > 0) {
			ex = ex - fx; // move along vector, we start _at_ the eye and move towards b
			ey = ey - fy;
			ez = ez - fz;
			lx = Math.floor(ex);
			ly = Math.floor(ey);
			lz = Math.floor(ez);
			if (lx == bx && ly == by && lz == bz) {
				return true; // we've reached our starting block, don't test it.
			}
			IBlockInfo between = NmsInstance.get().getBlockInfo(world, (int) lx, (int) ly, (int) lz);
			if (between != null && !ProximityHider.configManager.getConfig().isBlockTransparent(between.getCombinedId())) {
				return false; // fail on first hit, this ray is "blocked"
			}
			s--; // we stop
		}
		return true;
	}

	private static void restart() {
		synchronized (thread) {
			if (thread.isInterrupted() || !thread.isAlive()) {
				running = false;
			}

			if (!running && ProximityHider.configManager.getConfig().isProximityHiderEnabled()) {
				// Load ProximityHider
				ProximityHider.load();
			}
		}
	}

	public static void addProximityBlocks(Player player, int chunkX, int chunkZ, List<BlockCoords> blocks) {
		ProximityHiderConfig proximityHider = ProximityHider.configManager.getWorld(player.getWorld())
				.getProximityHiderConfig();

		if (!proximityHider.isEnabled()) {
			return;
		}

		restart();

		synchronized (proximityHiderTracker) {
			ProximityHiderPlayer playerInfo = proximityHiderTracker.get(player);
			World world = player.getWorld();

			if (playerInfo == null) {
				proximityHiderTracker.put(player, playerInfo = new ProximityHiderPlayer(world));
			} else if (!playerInfo.getWorld().equals(world)) {
				playerInfo.setWorld(world);
				playerInfo.clearChunks();
			}

			if (blocks.size() > 0) {
				playerInfo.putBlocks(chunkX, chunkZ, blocks);
			} else {
				playerInfo.removeChunk(chunkX, chunkZ);
			}
		}

		boolean isPlayerToReload;

		synchronized (playersToReload) {
			isPlayerToReload = playersToReload.remove(player);
		}

		if (isPlayerToReload) {
			addPlayerToCheck(player, null);
		}
	}

	public static void clearPlayer(Player player) {
		synchronized (proximityHiderTracker) {
			proximityHiderTracker.remove(player);
		}

		synchronized (playersToCheck) {
			playersToCheck.remove(player);
		}

		synchronized (playersToReload) {
			playersToReload.remove(player);
		}
	}

	public static void clearBlocksForOldWorld(Player player) {
		synchronized (proximityHiderTracker) {
			ProximityHiderPlayer playerInfo = proximityHiderTracker.get(player);

			if (playerInfo != null) {
				World world = player.getWorld();

				if (!playerInfo.getWorld().equals(world)) {
					playerInfo.setWorld(world);
					playerInfo.clearChunks();
				}
			}
		}
	}

	public static void addPlayerToCheck(Player player, Location location) {
		synchronized (playersToCheck) {
			if (!playersToCheck.containsKey(player)) {
				playersToCheck.put(player, location);
			}
		}
	}

	public static void addPlayersToReload(HashSet<Player> players) {
		if (!ProximityHider.configManager.getConfig().isProximityHiderEnabled()) {
			return;
		}

		synchronized (playersToReload) {
			playersToReload.addAll(players);
		}
	}
}