package net.imprex.orebfuscator;

import dev.imprex.orebfuscator.cache.AbstractRegionFileCache;
import dev.imprex.orebfuscator.config.api.Config;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.util.BlockPos;
import java.lang.reflect.Constructor;
import java.util.concurrent.CompletableFuture;
import net.imprex.orebfuscator.nms.NmsManager;
import net.imprex.orebfuscator.util.MinecraftVersion;
import net.imprex.orebfuscator.util.ServerVersion;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;

public class OrebfuscatorNms {

  private static NmsManager instance;

  public static void initialize() {
    if (OrebfuscatorNms.instance != null) {
      throw new IllegalStateException("NMS adapter is already initialized!");
    }

    String nmsVersion = MinecraftVersion.nmsVersion();
    if (ServerVersion.isMojangMapped() && MinecraftVersion.isBelow("26.0.0")) {
      nmsVersion += "_mojang";
    }

    OfcLogger.info("Searching NMS adapter for version=\"" + MinecraftVersion.current() +  "\", nms=\"" + nmsVersion + "\"!");

    try {
      String className = "net.imprex.orebfuscator.nms." + nmsVersion + ".NmsManager";
      Class<? extends NmsManager> nmsManager = Class.forName(className).asSubclass(NmsManager.class);
      Constructor<? extends NmsManager> constructor = nmsManager.getConstructor();
      OrebfuscatorNms.instance = constructor.newInstance();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Server version \"" + nmsVersion + "\" is currently not supported!", e);
    } catch (Exception e) {
      throw new RuntimeException("Couldn't initialize NMS adapter", e);
    }

    OfcLogger.info("NMS adapter for server version \"" + nmsVersion + "\" found!");
  }

  public static RegistryAccessor registry() {
    return instance;
  }

  public static AbstractRegionFileCache<?> createRegionFileCache(Config config) {
    return instance.createRegionFileCache(config);
  }

  public static CompletableFuture<@Nullable ChunkAccessor> getChunkFuture(World world, int chunkX, int chunkZ) {
    return instance.getChunkFuture(world, chunkX, chunkZ);
  }

  public static @Nullable ChunkAccessor getChunkNow(World world, int chunkX, int chunkZ) {
    return instance.getChunkNow(world, chunkX, chunkZ);
  }

  public static void sendBlockUpdates(World world, Iterable<BlockPos> iterable) {
    instance.sendBlockUpdates(world, iterable);
  }

  public static void sendBlockUpdates(Player player, Iterable<BlockPos> iterable) {
    instance.sendBlockUpdates(player, iterable);
  }
}