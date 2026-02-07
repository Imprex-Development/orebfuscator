package net.imprex.orebfuscator.iterop;

import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import dev.imprex.orebfuscator.config.api.WorldConfigBundle;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.interop.ChunkPacketAccessor;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.ChunkDirection;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.util.MinecraftVersion;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BukkitWorldAccessor implements WorldAccessor {

  private static final boolean HAS_DYNAMIC_HEIGHT = MinecraftVersion.isAtOrAbove("1.17");

  private static final @Nullable MethodAccessor WORLD_GET_MAX_HEIGHT = getWorldMethod("getMaxHeight");
  private static final @Nullable MethodAccessor WORLD_GET_MIN_HEIGHT = getWorldMethod("getMinHeight");

  private static @Nullable MethodAccessor getWorldMethod(String methodName) {
    if (HAS_DYNAMIC_HEIGHT) {
      MethodAccessor methodAccessor = getWorldMethod0(World.class, methodName);
      if (methodAccessor == null) {
        throw new RuntimeException("unable to find method: World::" + methodName + "()");
      }
      OfcLogger.debug("HeightAccessor found method: World::" + methodName + "()");
      return methodAccessor;
    }
    return null;
  }

  private static @Nullable MethodAccessor getWorldMethod0(Class<?> target, String methodName) {
    try {
      return Accessors.getMethodAccessor(target, methodName);
    } catch (IllegalArgumentException e) {
      for (Class<?> iterface : target.getInterfaces()) {
        MethodAccessor methodAccessor = getWorldMethod0(iterface, methodName);
        if (methodAccessor != null) {
          return methodAccessor;
        }
      }
    }
    return null;
  }

  private static int blockToSectionCoord(int block) {
    return block >> 4;
  }

  public final World world;
  private final Orebfuscator orebfuscator;

  private final int maxHeight;
  private final int minHeight;

  private @Nullable WorldConfigBundle worldConfigBundle;

  BukkitWorldAccessor(World world, Orebfuscator orebfuscator) {
    this.world = Objects.requireNonNull(world);
    this.orebfuscator = Objects.requireNonNull(orebfuscator);

    if (WORLD_GET_MAX_HEIGHT != null && WORLD_GET_MIN_HEIGHT != null) {
      this.maxHeight = (int) WORLD_GET_MAX_HEIGHT.invoke(world);
      this.minHeight = (int) WORLD_GET_MIN_HEIGHT.invoke(world);
    } else {
      this.maxHeight = 256;
      this.minHeight = 0;
    }
  }

  @Override
  public WorldConfigBundle config() {
    if (this.worldConfigBundle == null) {
      this.worldConfigBundle = this.orebfuscator.config().world(this);
    }
    return this.worldConfigBundle;
  }

  @Override
  public String name() {
    return this.world.getName();
  }

  @Override
  public int height() {
    return this.maxHeight - this.minHeight;
  }

  @Override
  public int minBuildHeight() {
    return this.minHeight;
  }

  @Override
  public int maxBuildHeight() {
    return this.maxHeight;
  }

  @Override
  public int sectionCount() {
    return this.maxSection() - this.minSection();
  }

  @Override
  public int minSection() {
    return blockToSectionCoord(this.minBuildHeight());
  }

  @Override
  public int maxSection() {
    return blockToSectionCoord(this.maxBuildHeight() - 1) + 1;
  }

  @Override
  public int sectionIndex(int y) {
    return blockToSectionCoord(y) - minSection();
  }

  public ChunkAccessor[] getNeighboringChunksNow(int chunkX, int chunkZ) {
    final ChunkAccessor[] chunks = new ChunkAccessor[4];

    for (ChunkDirection direction : ChunkDirection.values()) {
      int x = chunkX + direction.getOffsetX();
      int z = chunkZ + direction.getOffsetZ();
      chunks[direction.ordinal()] = getChunkNow(x, z);
    }

    return chunks;
  }

  @Override
  @SuppressWarnings("unchecked")
  public CompletableFuture<ChunkAccessor[]> getNeighboringChunks(ObfuscationRequest request) {
    var neighborChunks = request.neighborChunks();
    if (neighborChunks != null && Arrays.stream(neighborChunks).noneMatch(ChunkAccessor::isNullOrEmpty)) {
      return CompletableFuture.completedFuture(neighborChunks);
    }

    final ChunkPacketAccessor packet = request.packet();
    final CompletableFuture<ChunkAccessor>[] futures = (CompletableFuture<ChunkAccessor>[]) new CompletableFuture[4];

    for (ChunkDirection direction : ChunkDirection.values()) {
      int chunkX = packet.chunkX() + direction.getOffsetX();
      int chunkZ = packet.chunkZ() + direction.getOffsetZ();

      int index = direction.ordinal();
      var chunk = neighborChunks != null ? neighborChunks[index] : null;

      if (ChunkAccessor.isNullOrEmpty(chunk)) {
        futures[index] = OrebfuscatorNms.getChunkFuture(world, chunkX, chunkZ)
            .thenApply(ChunkAccessor::ofNullable);
      } else {
        futures[index] = CompletableFuture.completedFuture(chunk);
      }
    }

    return CompletableFuture.allOf(futures).thenApply(v ->
        Arrays.stream(futures).map(CompletableFuture::join).toArray(ChunkAccessor[]::new));
  }

  @Override
  public ChunkAccessor getChunkNow(int chunkX, int chunkZ) {
    ChunkAccessor chunkAccessor = OrebfuscatorNms.getChunkNow(world, chunkX, chunkZ);
    return ChunkAccessor.ofNullable(chunkAccessor);
  }

  @Override
  public void sendBlockUpdates(Iterable<BlockPos> iterable) {
    OrebfuscatorNms.sendBlockUpdates(world, iterable);
  }

  @Override
  public int hashCode() {
    return this.world.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    return obj instanceof BukkitWorldAccessor other && this.world.equals(other.world);
  }

  @Override
  public String toString() {
    return "[name=%s, minY=%s, maxY=%s]".formatted(name(), minHeight, maxHeight);
  }
}
