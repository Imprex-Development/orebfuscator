package net.imprex.orebfuscator.proximity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.joml.FrustumIntersection;
import org.joml.Quaternionf;

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

	protected void process(List<Player> players) {
		for (Player player : players) {
			try {
				this.process(player);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void process(Player player) {
		if (this.shouldIgnorePlayer(player)) {
			return;
		}

		World world = player.getWorld();

		// check if world has enabled proximity config
		ProximityConfig proximityConfig = this.config.world(world).proximity();
		if (proximityConfig == null || !proximityConfig.isEnabled()) {
			return;
		}

		// frustum culling and ray casting both need rotation changes
		boolean needsRotation = proximityConfig.frustumCullingEnabled() || proximityConfig.useRayCastCheck();

		// check if player changed location since last time
		OrebfuscatorPlayer orebfuscatorPlayer = this.playerMap.get(player);
		if (orebfuscatorPlayer == null || !orebfuscatorPlayer.needsProximityUpdate(needsRotation)) {
			return;
		}

		int distance = proximityConfig.distance();
		int distanceSquared = distance * distance;

		List<BlockPos> updateBlocks = new ArrayList<>();
		Location eyeLocation = needsRotation
				? player.getEyeLocation()
				: null;

		// create frustum planes if culling is enabled
		FrustumIntersection frustum = proximityConfig.frustumCullingEnabled()
				? new FrustumIntersection(proximityConfig.frustumCullingProjectionMatrix()
						.rotate(new Quaternionf()
								.rotateX((float) Math.toRadians(eyeLocation.getPitch()))
								.rotateY((float) Math.toRadians(eyeLocation.getYaw() + 180)))
						.translate(
								(float) -eyeLocation.getX(),
								(float) -eyeLocation.getY(),
								(float) -eyeLocation.getZ()
						), false)
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
					BlockPos blockPos = iterator.next();

					// check if block is in range
					double blockDistanceSquared = blockPos.distanceSquared(location.getX(), location.getY(), location.getZ());
					if (blockDistanceSquared > distanceSquared) {
						continue;
					}

					// do frustum culling check
					if (proximityConfig.frustumCullingEnabled() && blockDistanceSquared > proximityConfig.frustumCullingMinDistanceSquared()) {

						// check if block AABB is inside frustum
						int result = frustum.intersectAab(
								blockPos.x, blockPos.y, blockPos.z,
								blockPos.x + 1, blockPos.y + 1, blockPos.z + 1);

						// block is outside
						if (result != FrustumIntersection.INSIDE && result != FrustumIntersection.INTERSECT) {
							continue;
						}
					}

					// do ray cast check
					if (proximityConfig.useRayCastCheck() && !MathUtil.doFastCheck(blockPos, eyeLocation, world)) {
						continue;
					}

					// block is visible and needs update
					iterator.remove();
					updateBlocks.add(blockPos);
				}

				if (blocks.isEmpty()) {
					orebfuscatorPlayer.removeChunk(chunkX, chunkZ);
				}
			}
		}

		Bukkit.getScheduler().runTask(this.orebfuscator, () -> {
			if (player.isOnline() && player.getWorld().equals(world)) {
				NmsInstance.sendBlockUpdates(player, updateBlocks);
			}
		});
	}
}
