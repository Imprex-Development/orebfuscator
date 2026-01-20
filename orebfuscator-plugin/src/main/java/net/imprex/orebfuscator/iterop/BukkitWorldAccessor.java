package net.imprex.orebfuscator.iterop;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jspecify.annotations.NullMarked;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;
import dev.imprex.orebfuscator.config.api.WorldConfigBundle;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.ChunkDirection;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorCompatibility;
import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.util.MinecraftVersion;

@NullMarked
public class BukkitWorldAccessor implements WorldAccessor {

  private static final boolean HAS_DYNAMIC_HEIGHT = MinecraftVersion.isAtOrAbove("1.17");

  private static final Map<World, BukkitWorldAccessor> ACCESSOR_LOOKUP = new ConcurrentHashMap<>();

  public static BukkitWorldAccessor get(World world) {
    return ACCESSOR_LOOKUP.computeIfAbsent(world, key -> {
      throw new IllegalStateException("Created world accessor outside of event!");
    });
  }

  private static final MethodAccessor WORLD_GET_MAX_HEIGHT = getWorldMethod("getMaxHeight");
  private static final MethodAccessor WORLD_GET_MIN_HEIGHT = getWorldMethod("getMinHeight");

  private static MethodAccessor getWorldMethod(String methodName) {
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

  private static MethodAccessor getWorldMethod0(Class<?> target, String methodName) {
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

  public static Collection<BukkitWorldAccessor> getWorlds() {
    return ACCESSOR_LOOKUP.values();
  }

  public static void registerListener(Orebfuscator orebfuscator) {
    Bukkit.getPluginManager().registerEvents(new Listener() {
      @EventHandler
      public void onWorldUnload(WorldLoadEvent event) {
        World world = event.getWorld();
        ACCESSOR_LOOKUP.put(world, new BukkitWorldAccessor(world, orebfuscator));
      }

      @EventHandler
      public void onWorldUnload(WorldUnloadEvent event) {
        ACCESSOR_LOOKUP.remove(event.getWorld());
      }
    }, orebfuscator);

    for (World world : Bukkit.getWorlds()) {
      ACCESSOR_LOOKUP.put(world, new BukkitWorldAccessor(world, orebfuscator));
    }
  }

  public final World world;
  private final Orebfuscator orebfuscator;

  private final int maxHeight;
  private final int minHeight;

  private WorldConfigBundle worldConfigBundle;

  private BukkitWorldAccessor(World world, Orebfuscator orebfuscator) {
    this.world = Objects.requireNonNull(world);
    this.orebfuscator = Objects.requireNonNull(orebfuscator);

    if (HAS_DYNAMIC_HEIGHT) {
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
  public String getName() {
    return this.world.getName();
  }

  @Override
  public int getHeight() {
    return this.maxHeight - this.minHeight;
  }

  @Override
  public int getMinBuildHeight() {
    return this.minHeight;
  }

  @Override
  public int getMaxBuildHeight() {
    return this.maxHeight;
  }

  @Override
  public int getSectionCount() {
    return this.getMaxSection() - this.getMinSection();
  }

  @Override
  public int getMinSection() {
    return blockToSectionCoord(this.getMinBuildHeight());
  }

  @Override
  public int getMaxSection() {
    return blockToSectionCoord(this.getMaxBuildHeight() - 1) + 1;
  }

  @Override
  public int getSectionIndex(int y) {
    return blockToSectionCoord(y) - getMinSection();
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
    return String.format("[%s, minY=%s, maxY=%s]", world.getName(), minHeight, maxHeight);
  }
}
