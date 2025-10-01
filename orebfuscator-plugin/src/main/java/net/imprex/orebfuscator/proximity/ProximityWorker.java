package net.imprex.orebfuscator.proximity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.joml.FrustumIntersection;
import org.joml.Quaternionf;

import dev.imprex.orebfuscator.config.OrebfuscatorConfig;
import dev.imprex.orebfuscator.config.api.ProximityConfig;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorCompatibility;
import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.iterop.BukkitWorldAccessor;
import net.imprex.orebfuscator.player.OrebfuscatorPlayer;
import net.imprex.orebfuscator.player.OrebfuscatorPlayerChunk;
import net.imprex.orebfuscator.player.OrebfuscatorPlayerMap;
import net.imprex.orebfuscator.util.FastGazeUtil;
import net.imprex.orebfuscator.util.PermissionUtil;

public class ProximityWorker {

  private final OrebfuscatorConfig config;
  private final OrebfuscatorPlayerMap playerMap;

  public ProximityWorker(Orebfuscator orebfuscator) {
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
    WorldAccessor worldAccessor = BukkitWorldAccessor.get(world);

    // check if world has enabled proximity config
    ProximityConfig proximityConfig = this.config.world(worldAccessor).proximity();
    if (proximityConfig == null || !proximityConfig.isEnabled()) {
      return;
    }

    // frustum culling and ray casting both need rotation changes
    boolean needsRotation = proximityConfig.frustumCullingEnabled() || proximityConfig.rayCastCheckEnabled();

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

        OrebfuscatorPlayerChunk chunk = orebfuscatorPlayer.getChunk(chunkX, chunkZ);
        if (chunk == null) {
          continue;
        }

        for (Iterator<BlockPos> iterator = chunk.proximityIterator(); iterator.hasNext(); ) {
          BlockPos blockPos = iterator.next();

          // check if block is in range
          double blockDistanceSquared = blockPos.distanceSquared(location.getX(), location.getY(), location.getZ());
          if (blockDistanceSquared > distanceSquared) {
            continue;
          }

          // do frustum culling check
          if (proximityConfig.frustumCullingEnabled()
              && blockDistanceSquared > proximityConfig.frustumCullingMinDistanceSquared()) {

            // check if block AABB is inside frustum
            int result = frustum.intersectAab(
                blockPos.x(), blockPos.y(), blockPos.z(),
                blockPos.x() + 1, blockPos.y() + 1, blockPos.z() + 1);

            // block is outside
            if (result != FrustumIntersection.INSIDE && result != FrustumIntersection.INTERSECT) {
              continue;
            }
          }

          // do ray cast check
          if (proximityConfig.rayCastCheckEnabled() && !FastGazeUtil.doFastCheck(blockPos, eyeLocation, world,
              proximityConfig.rayCastCheckOnlyCheckCenter())) {
            continue;
          }

          // block is visible and needs update
          iterator.remove();
          updateBlocks.add(blockPos);
        }

        if (chunk.isEmpty()) {
          orebfuscatorPlayer.removeChunk(chunkX, chunkZ);
        }
      }
    }

    OrebfuscatorCompatibility.runForPlayer(player, () -> {
      if (player.isOnline() && player.getWorld().equals(world)) {
        OrebfuscatorNms.sendBlockUpdates(player, updateBlocks);
      }
    });
  }
}
