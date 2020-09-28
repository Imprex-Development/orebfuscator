package net.imprex.orebfuscator.proximityhider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.ProximityConfig;
import net.imprex.orebfuscator.util.BlockCoords;
import net.imprex.orebfuscator.util.MathUtil;
import net.imprex.orebfuscator.util.OFCLogger;

public class ProximityThread extends Thread {

	private final Orebfuscator orebfuscator;
	private final OrebfuscatorConfig config;

	private final ProximityHider proximityHider;
	private final AtomicBoolean running = new AtomicBoolean(true);

	public ProximityThread(ProximityHider proximityHider, Orebfuscator orebfuscator) {
		this.proximityHider = proximityHider;
		this.orebfuscator = orebfuscator;
		this.config = orebfuscator.getOrebfuscatorConfig();
	}

	@Override
	public void run() {
		while (this.running.get()) {
			Player player = this.proximityHider.pollPlayer();

			try {
				if (player == null || !player.isOnline()) {
					continue;
				}

				Location location = player.getLocation();
				World world = location.getWorld();

				ProximityConfig proximityConfig = this.config.proximity(world);
				ProximityPlayerData proximityPlayer = this.proximityHider.getPlayer(player);
				if (proximityPlayer == null || proximityConfig == null || !proximityConfig.enabled() || !proximityPlayer.getWorld().equals(world)) {
					continue;
				}

				int distance = proximityConfig.distance();
				int distanceSquared = proximityConfig.distanceSquared();

				List<BlockCoords> updateBlocks = new ArrayList<>();
				Location eyeLocation = player.getEyeLocation();

				int minChunkX = (location.getBlockX() - distance) >> 4;
				int maxChunkX = (location.getBlockX() + distance) >> 4;
				int minChunkZ = (location.getBlockZ() - distance) >> 4;
				int maxChunkZ = (location.getBlockZ() + distance) >> 4;

				for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
					for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
						Set<BlockCoords> blocks = proximityPlayer.getBlocks(chunkX, chunkZ);

						if (blocks == null) {
							continue;
						}

						for (Iterator<BlockCoords> iterator = blocks.iterator(); iterator.hasNext(); ) {
							BlockCoords blockCoords = iterator.next();
							Location blockLocation = new Location(world, blockCoords.x, blockCoords.y, blockCoords.z);

							if (location.distanceSquared(blockLocation) < distanceSquared) {
								if (!proximityConfig.useFastGazeCheck() || MathUtil.doFastCheck(blockLocation, eyeLocation, world)) {
									iterator.remove();
									updateBlocks.add(blockCoords);
								}
							}
						}

						if (blocks.isEmpty()) {
							proximityPlayer.removeChunk(chunkX, chunkZ);
						}
					}
				}

				Bukkit.getScheduler().runTask(this.orebfuscator, () -> {
					if (player.isOnline()) {
						for (BlockCoords blockCoords : updateBlocks) {
							if (NmsInstance.sendBlockChange(player, blockCoords)) {
								NmsInstance.updateBlockTileEntity(player, blockCoords);
							}
						}
					}
				});
			} catch (Exception e) {
				OFCLogger.err(e);
			} finally {
				this.proximityHider.unlockPlayer(player);
			}
		}
	}

	public void destroy() {
		this.running.set(false);
	}
}