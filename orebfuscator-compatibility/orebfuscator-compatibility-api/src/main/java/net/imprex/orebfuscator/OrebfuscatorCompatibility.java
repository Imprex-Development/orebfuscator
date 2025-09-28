package net.imprex.orebfuscator;

import java.lang.reflect.Constructor;
import java.util.concurrent.CompletableFuture;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.imprex.orebfuscator.compatibility.CompatibilityLayer;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.util.MinecraftVersion;
import net.imprex.orebfuscator.util.OFCLogger;
import net.imprex.orebfuscator.util.ServerVersion;

public class OrebfuscatorCompatibility {

  private static CompatibilityLayer instance;

  public static void initialize(Plugin plugin, Config config) {
    if (OrebfuscatorCompatibility.instance != null) {
      throw new IllegalStateException("Compatibility layer is already initialized!");
    }

    String className = "net.imprex.orebfuscator.compatibility.bukkit.BukkitCompatibilityLayer";
    if (ServerVersion.isFolia()) {
      className = "net.imprex.orebfuscator.compatibility.folia.FoliaCompatibilityLayer";
    } else if (ServerVersion.isPaper() && MinecraftVersion.minorVersion() >= 13) {
      className = "net.imprex.orebfuscator.compatibility.paper.PaperCompatibilityLayer";
    }

    try {
      OFCLogger.debug("Loading compatibility layer for: " + className);
      Class<? extends CompatibilityLayer> nmsManager = Class.forName(className).asSubclass(CompatibilityLayer.class);
      Constructor<? extends CompatibilityLayer> constructor = nmsManager.getConstructor(Plugin.class, Config.class);
      OrebfuscatorCompatibility.instance = constructor.newInstance(plugin, config);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Compatibility layer is missing", e);
    } catch (Exception e) {
      throw new RuntimeException("Couldn't initialize compatibility layer", e);
    }

    OFCLogger.debug("Compatibility layer successfully loaded");
  }

  public static boolean isGameThread() {
    return instance.isGameThread();
  }

  public static void runForPlayer(Player player, Runnable runnable) {
    instance.getScheduler().runForPlayer(player, runnable);
  }

  public static void runAsyncNow(Runnable runnable) {
    instance.getScheduler().runAsyncNow(runnable);
  }

  public static void runAsyncAtFixedRate(Runnable runnable, long delay, long period) {
    instance.getScheduler().runAsyncAtFixedRate(runnable, delay, period);
  }

  public static void cancelTasks() {
    instance.getScheduler().cancelTasks();
  }

  public static CompletableFuture<ReadOnlyChunk[]> getNeighboringChunks(World world, ChunkPosition position) {
    return instance.getNeighboringChunks(world, position);
  }

  public static void close() {
    if (instance != null) {
      instance.getScheduler().cancelTasks();
      instance = null;
    }
  }
}
