package net.imprex.orebfuscator.iterop;

import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import dev.imprex.orebfuscator.config.api.WorldConfigBundle;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.ChunkDirection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorCompatibility;
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

  public ChunkAccessor[] getNeighboringChunks(int chunkX, int chunkZ) {
    ChunkAccessor[] neighboringChunks = new ChunkAccessor[4];

    for (ChunkDirection direction : ChunkDirection.values()) {
      int x = chunkX + direction.getOffsetX();
      int z = chunkZ + direction.getOffsetZ();
      int index = direction.ordinal();

      neighboringChunks[index] = OrebfuscatorNms.tryGetChunkAccessor(world, x, z);
    }

    return neighboringChunks;
  }

  @Override
  public CompletableFuture<ChunkAccessor[]> getNeighboringChunks(ObfuscationRequest request) {
    return OrebfuscatorCompatibility.getNeighboringChunks(world, request);
  }

  @Override
  public ChunkAccessor getChunk(int chunkX, int chunkZ) {
    return OrebfuscatorNms.getChunkAccessor(world, chunkX, chunkZ);
  }

  @Override
  public int getBlockState(int x, int y, int z) {
    return OrebfuscatorNms.getBlockState(world, x, y, z);
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
    return "[minY=%s, maxY=%s]".formatted(minHeight, maxHeight);
  }
}
