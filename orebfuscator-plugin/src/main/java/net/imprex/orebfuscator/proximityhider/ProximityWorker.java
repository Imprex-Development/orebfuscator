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

import com.google.common.base.Objects;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorPlayer;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.ProximityConfig;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.MathUtil;
import net.imprex.orebfuscator.util.PermissionUtil;

public class ProximityWorker {

	private final Orebfuscator orebfuscator;
	private final OrebfuscatorConfig config;

	public ProximityWorker(Orebfuscator orebfuscator) {
		this.orebfuscator = orebfuscator;
		this.config = orebfuscator.getOrebfuscatorConfig();
	}

	private boolean shouldIgnorePlayer(Player player) {
		if (PermissionUtil.canBypassObfuscate(player)) {
			return true;
		}

		return player.getGameMode() == GameMode.SPECTATOR && this.config.general().ignoreSpectator();
	}

	private boolean isLocationSimilar(boolean rotation, Location a, Location b) {
		// check if world changed
		if (!Objects.equal(a.getWorld(), b.getWorld())) {
			return false;
		}

		// check if len(xyz) changed less then 0.5 blocks
		if (a.distanceSquared(b) > 0.25) {
			return false;
		}

		// check if rotation changed less then 10deg yaw or 5deg pitch
		if (rotation && (Math.abs(a.getYaw() - b.getYaw()) > 10 || Math.abs(a.getPitch() - b.getPitch()) > 5)) {
			return false;
		}

		return true;
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
		OrebfuscatorPlayer orebfuscatorPlayer = OrebfuscatorPlayer.get(player);
		if (!orebfuscatorPlayer.needsProximityUpdate((a, b) -> isLocationSimilar(proximityConfig.useFastGazeCheck(), a, b))) {
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
			if (player.isOnline()) {
				for (BlockPos blockCoords : updateBlocks) {
					NmsInstance.sendBlockChange(player, blockCoords);
				}
			}
		});
	}
}
