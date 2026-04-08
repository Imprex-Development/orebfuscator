package net.imprex.orebfuscator;

import dev.imprex.orebfuscator.config.api.Config;
import dev.imprex.orebfuscator.logging.OfcLogger;
import java.lang.reflect.Constructor;
import net.imprex.orebfuscator.compatibility.CompatibilityLayer;
import net.imprex.orebfuscator.util.ServerVersion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class OrebfuscatorCompatibility {

  private static CompatibilityLayer instance;

  public static void initialize(Plugin plugin, Config config) {
    if (OrebfuscatorCompatibility.instance != null) {
      throw new IllegalStateException("Compatibility layer is already initialized!");
    }

    String className = "net.imprex.orebfuscator.compatibility.spigot.SpigotCompatibilityLayer";
    if (ServerVersion.isFolia()) {
      className = "net.imprex.orebfuscator.compatibility.folia.FoliaCompatibilityLayer";
    } else if (ServerVersion.isPaper()) {
      className = "net.imprex.orebfuscator.compatibility.paper.PaperCompatibilityLayer";
    }

    try {
      OfcLogger.debug("Loading compatibility layer for: " + className);
      Class<? extends CompatibilityLayer> nmsManager = Class.forName(className).asSubclass(CompatibilityLayer.class);
      Constructor<? extends CompatibilityLayer> constructor = nmsManager.getConstructor(Plugin.class, Config.class);
      OrebfuscatorCompatibility.instance = constructor.newInstance(plugin, config);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Compatibility layer is missing", e);
    } catch (Exception e) {
      throw new RuntimeException("Couldn't initialize compatibility layer", e);
    }

    OfcLogger.debug("Compatibility layer successfully loaded");
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

  public static void close() {
    if (instance != null) {
      instance.getScheduler().cancelTasks();
      instance = null;
    }
  }
}
