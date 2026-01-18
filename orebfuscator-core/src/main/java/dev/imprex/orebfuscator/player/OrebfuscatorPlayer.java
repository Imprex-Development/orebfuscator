package dev.imprex.orebfuscator.player;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import dev.imprex.orebfuscator.config.api.AdvancedConfig;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.interop.PlayerAccessor;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.util.EntityPose;

@NullMarked
public class OrebfuscatorPlayer {

  private final AdvancedConfig config;
  private final PlayerAccessor player;

  private final AtomicReference<@Nullable WorldAccessor> world = new AtomicReference<>();
  private final Map<Long, OrebfuscatorPlayerChunk> chunks = new ConcurrentHashMap<>();

  private volatile long latestUpdateTimestamp = System.currentTimeMillis();
  private volatile EntityPose location = EntityPose.ZERO;

  public OrebfuscatorPlayer(OrebfuscatorCore orebfuscator, PlayerAccessor player) {
    this.config = orebfuscator.config().advanced();
    this.player = player;
  }

  /**
   * Returns true if the last proximity update is longer ago then the configured proximity player interval (default 5s)
   * or if the players location since the last update change according to the given rotation boolean and the
   * {@link OrebfuscatorPlayer#isLocationSimilar isLocationSimilar} method.
   *
   * @param rotation passed to the <code>isLocationSimilar</code> method
   * @return true if a proximity update is needed
   */
  public boolean needsProximityUpdate(boolean rotation) {
    if (!player.isAlive()) {
      return false;
    }

    long timestamp = System.currentTimeMillis();
    if (this.config.hasProximityPlayerCheckInterval()
        && timestamp - this.latestUpdateTimestamp > this.config.proximityPlayerCheckInterval()) {

      // always update location + latestUpdateTimestamp on update
      this.location = player.pose();
      this.latestUpdateTimestamp = timestamp;

      return true;
    }

    EntityPose location = player.pose();
    if (isLocationSimilar(rotation, this.location, location)) {
      return false;
    }

    // always update location + latestUpdateTimestamp on update
    this.location = location;
    this.latestUpdateTimestamp = timestamp;

    return true;
  }

  /**
   * Returns true if the worlds are the same and the distance between the locations is less then 0.5. If the rotation
   * boolean is set this method also check if the yaw changed less then 5deg and the pitch less then 2.5deg.
   *
   * @param rotation should rotation be checked
   * @param a        the first location
   * @param b        the second location
   * @return if the locations are similar
   */
  private static boolean isLocationSimilar(boolean rotation, EntityPose a, EntityPose b) {
    // check if world changed
    if (!Objects.equals(a.world(), b.world())) {
      return false;
    }

    // check if len(xyz) changed less then 0.5 blocks
    if (a.distanceSquared(b) > 0.25) {
      return false;
    }

    // check if rotation changed less then 5deg yaw or 2.5deg pitch
    if (rotation && (Math.abs(a.rotY() - b.rotY()) > 5 || Math.abs(a.rotX() - b.rotX()) > 2.5)) {
      return false;
    }

    return true;
  }

  public void clearChunks() {
    WorldAccessor world = player.world();
    if (!Objects.equals(this.world.getAndSet(world), world)) {
      this.chunks.clear();
    }
  }

  public void addChunk(WorldAccessor world, int chunkX, int chunkZ, List<ProximityBlock> blocks) {
    if (Objects.equals(this.world.getAcquire(), world)) {
      long key = ChunkAccessor.chunkCoordsToLong(chunkX, chunkZ);
      this.chunks.put(key, new OrebfuscatorPlayerChunk(chunkX, chunkZ, blocks));
    }
  }

  public OrebfuscatorPlayerChunk getChunk(WorldAccessor world, int chunkX, int chunkZ) {
    if (Objects.equals(this.world.getAcquire(), world)) {
      long key = ChunkAccessor.chunkCoordsToLong(chunkX, chunkZ);
      return this.chunks.get(key);
    } else {
      return null;
    }
  }

  public void removeChunk(WorldAccessor world, int chunkX, int chunkZ) {
    if (Objects.equals(this.world.getAcquire(), world)) {
      long key = ChunkAccessor.chunkCoordsToLong(chunkX, chunkZ);
      this.chunks.remove(key);
    }
  }
}
