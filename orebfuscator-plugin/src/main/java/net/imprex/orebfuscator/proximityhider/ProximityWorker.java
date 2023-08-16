package net.imprex.orebfuscator.proximityhider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.ProximityConfig;
import net.imprex.orebfuscator.player.OrebfuscatorPlayer;
import net.imprex.orebfuscator.player.OrebfuscatorPlayerMap;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.MathUtil;
import net.imprex.orebfuscator.util.PermissionUtil;

public class ProximityWorker {

	private final Orebfuscator orebfuscator;
	private final OrebfuscatorConfig config;
	private final OrebfuscatorPlayerMap playerMap;

	public ProximityWorker(Orebfuscator orebfuscator) {
		this.orebfuscator = orebfuscator;
		this.config = orebfuscator.getOrebfuscatorConfig();
		this.playerMap = orebfuscator.getPlayerMap();
	}

	private boolean shouldIgnorePlayer(Player player) {
		if (PermissionUtil.canBypassObfuscate(player)) {
			return true;
		}

		return player.getGameMode() == GameMode.SPECTATOR && this.config.general().ignoreSpectator();
	}

	protected void process(Player player) {
		if (this.shouldIgnorePlayer(player)) {
			return;
		}

		World world = player.getWorld();

		// check if world has enabled proximity config
		ProximityConfig proximityConfig = this.config.world(world).proximity();
		if (proximityConfig == null || !proximityConfig.isEnabled()) {
			return;
		}

		// check if player changed location since last time
		OrebfuscatorPlayer orebfuscatorPlayer = this.playerMap.get(player);
		if (orebfuscatorPlayer == null || !orebfuscatorPlayer.needsProximityUpdate(proximityConfig.useFastGazeCheck())) {
			return;
		}

		int distance = proximityConfig.distance();
		int distanceSquared = distance * distance;

		List<BlockPos> updateBlocks = new ArrayList<>();
		Location eyeLocation = proximityConfig.useFastGazeCheck()
				? player.getEyeLocation()
				: null;

		Location location = player.getLocation();
		int minChunkX = (location.getBlockX() - distance) >> 4;
		int maxChunkX = (location.getBlockX() + distance) >> 4;
		int minChunkZ = (location.getBlockZ() - distance) >> 4;
		int maxChunkZ = (location.getBlockZ() + distance) >> 4;

		for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
			for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {

				Set<BlockPos> blocks = orebfuscatorPlayer.getChunk(chunkX, chunkZ);
				if (blocks == null) {
					continue;
				}

				for (Iterator<BlockPos> iterator = blocks.iterator(); iterator.hasNext(); ) {
					BlockPos blockCoords = iterator.next();
					Location blockLocation = new Location(world, blockCoords.x, blockCoords.y, blockCoords.z);

					if (location.distanceSquared(blockLocation) < distanceSquared) {
						if (!proximityConfig.useFastGazeCheck() || MathUtil.doFastCheck(blockLocation, eyeLocation, world)) {
							iterator.remove();
							updateBlocks.add(blockCoords);
						}
					}
				}

				if (blocks.isEmpty()) {
					orebfuscatorPlayer.removeChunk(chunkX, chunkZ);
				}
			}
		}

		Bukkit.getScheduler().runTask(this.orebfuscator, () -> {
			if (player.isOnline() && player.getWorld().equals(world)) {
				for (BlockPos blockCoords : updateBlocks) {
					NmsInstance.sendBlockChange(player, blockCoords);
				}
			}
		});
	}
}
