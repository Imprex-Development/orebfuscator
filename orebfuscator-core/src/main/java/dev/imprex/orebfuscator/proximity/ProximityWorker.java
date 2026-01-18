package dev.imprex.orebfuscator.proximity;

import java.util.ArrayList;
import java.util.List;
import org.joml.FrustumIntersection;
import org.joml.Quaternionf;
import dev.imprex.orebfuscator.PermissionRequirements;
import dev.imprex.orebfuscator.config.OrebfuscatorConfig;
import dev.imprex.orebfuscator.config.api.ProximityConfig;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.interop.PlayerAccessor;
import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.player.OrebfuscatorPlayer;
import dev.imprex.orebfuscator.player.OrebfuscatorPlayerChunk;
import dev.imprex.orebfuscator.player.ProximityBlock;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.EntityPose;

public class ProximityWorker {

  private final OrebfuscatorConfig config;
  private final RegistryAccessor registry;

  public ProximityWorker(OrebfuscatorCore orebfuscator) {
    this.config = orebfuscator.config();
    this.registry = orebfuscator.registry();
  }

  private boolean shouldIgnorePlayer(PlayerAccessor player) {
    if (player.hasPermission(PermissionRequirements.BYPASS)) {
      return true;
    }

    return player.isSpectator() && this.config.general().ignoreSpectator();
  }

  protected void process(List<PlayerAccessor> players) {
    for (PlayerAccessor player : players) {
      try {
        this.process(player);
      } catch (Exception e) {
        OfcLogger.error(e);
      }
    }
  }

  private void process(PlayerAccessor player) {
    if (this.shouldIgnorePlayer(player)) {
      return;
    }

    var world = player.world();

    // check if level has enabled proximity config
    ProximityConfig proximityConfig = world.config().proximity();
    if (proximityConfig == null || !proximityConfig.isEnabled()) {
      return;
    }

    // frustum culling and ray casting both need rotation changes
    boolean needsRotation = proximityConfig.frustumCullingEnabled() || proximityConfig.rayCastCheckEnabled();

    // check if player changed location since last time
    OrebfuscatorPlayer orebfuscatorPlayer = player.orebfuscatorPlayer();
    if (!orebfuscatorPlayer.needsProximityUpdate(needsRotation)) {
      return;
    }

    int distance = proximityConfig.distance();
    int distanceSquared = distance * distance;

    List<BlockPos> updateBlocks = new ArrayList<>();
    EntityPose eyeLocation = player.eyePose();

    // create frustum planes if culling is enabled
    FrustumIntersection frustum =
        proximityConfig.frustumCullingEnabled()
            ? new FrustumIntersection(proximityConfig.frustumCullingProjectionMatrix()
                .rotate(new Quaternionf()
                    .rotateX((float) Math.toRadians(eyeLocation.rotX()))
                    .rotateY((float) Math.toRadians(eyeLocation.rotY() + 180)))
                .translate((float) -eyeLocation.x(), (float) -eyeLocation.y(), (float) -eyeLocation.z()), false)
            : null;

    EntityPose location = player.pose();
    int minChunkX = (location.blockX() - distance) >> 4;
    int maxChunkX = (location.blockX() + distance) >> 4;
    int minChunkZ = (location.blockZ() - distance) >> 4;
    int maxChunkZ = (location.blockZ() + distance) >> 4;
    
    ChunkAccessor playerChunk = world.getChunk(location.blockX() >> 4, location.blockZ() >> 4);
    int eyeBlockId = playerChunk.getBlockState(location.blockX(), eyeLocation.blockY(), location.blockZ());
    boolean isInLava = eyeBlockId >= 0 && this.registry.isLava(eyeBlockId);
    
    double lavaDistance = player.lavaFogDistance();
    double lavaDistanceSquared = lavaDistance * lavaDistance;
    
    ProximityRayCaster rayCaster = proximityConfig.rayCastCheckEnabled()
        ? new ProximityRayCaster(registry, world) : null;

    for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
      for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {

        OrebfuscatorPlayerChunk chunk = orebfuscatorPlayer.getChunk(world, chunkX, chunkZ);
        if (chunk == null) {
          continue;
        }

        try (var iterator = chunk.proximityIterator()) {
          while (iterator.hasNext()) {
            ProximityBlock proximityBlock = iterator.next();
            BlockPos blockPos = proximityBlock.blockPos();
            
            // skip lava obfuscated if not in lava
            // TODO: the current deobfuscation would only deobfuscate lava if the neighoring block is a hiddenBlock
            if (proximityBlock.lavaObfuscated() && !isInLava) {
              continue;
            }
            
            double compareDistanceSquared;
            double blockDistanceSquared;
            if (proximityBlock.lavaObfuscated()) {
              blockDistanceSquared = blockPos.distanceSquared(eyeLocation.x(), eyeLocation.y(), eyeLocation.z());
              compareDistanceSquared = lavaDistanceSquared;
            } else {
              blockDistanceSquared = blockPos.distanceSquared(location.x(), location.y(), location.z());
              compareDistanceSquared = distanceSquared;
            }

            // check if block is in range
            if (blockDistanceSquared > compareDistanceSquared) {
              continue;
            }

            // do frustum culling check
            if (proximityConfig.frustumCullingEnabled()
                && blockDistanceSquared > proximityConfig.frustumCullingMinDistanceSquared()) {

              // check if block AABB is inside frustum
              int result = frustum.intersectAab(blockPos.x(), blockPos.y(), blockPos.z(), blockPos.x() + 1,
                  blockPos.y() + 1, blockPos.z() + 1);

              // block is outside
              if (result != FrustumIntersection.INSIDE && result != FrustumIntersection.INTERSECT) {
                continue;
              }
            }

            // do ray cast check
            if (rayCaster != null && !rayCaster.isVisible(eyeLocation, blockPos)) {
              continue;
            }

            // block is visible and needs update
            iterator.remove();
            updateBlocks.add(blockPos);
          }
        }

        if (chunk.isEmpty()) {
          orebfuscatorPlayer.removeChunk(world, chunkX, chunkZ);
        }
      }
    }

    player.runForPlayer(() -> {
      if (player.isAlive() && player.world().equals(world)) {
        player.sendBlockUpdates(updateBlocks);
      }
    });
  }
}
