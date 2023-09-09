package net.imprex.orebfuscator;

import java.lang.reflect.Constructor;
import java.util.concurrent.CompletableFuture;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.imprex.orebfuscator.compatibility.CompatibilityLayer;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.util.OFCLogger;

public class OrebfuscatorCompatibility {

	private static final boolean IS_FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");
	private static final boolean IS_PAPER = !IS_FOLIA && classExists("com.destroystokyo.paper.PaperConfig");

	private static CompatibilityLayer instance;

	private static boolean classExists(String className) {
		try {
			Class.forName(className);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	public static void initialize(Plugin plugin) {
		if (OrebfuscatorCompatibility.instance != null) {
			throw new IllegalStateException("Compatibility layer is already initialized!");
		}

		String className = "net.imprex.orebfuscator.compatibility.bukkit.BukkitCompatibilityLayer";
		if (IS_FOLIA) {
			className = "net.imprex.orebfuscator.compatibility.folia.FoliaCompatibilityLayer";
		} else if (IS_PAPER) {
			className = "net.imprex.orebfuscator.compatibility.paper.PaperCompatibilityLayer";
		}

		try {
			OFCLogger.debug("Loading compatibility layer for: " + className);
			Class<? extends CompatibilityLayer> nmsManager = Class.forName(className).asSubclass(CompatibilityLayer.class);
			Constructor<? extends CompatibilityLayer> constructor = nmsManager.getConstructor(Plugin.class);
			OrebfuscatorCompatibility.instance = constructor.newInstance(plugin);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Compatibility layer is missing", e);
		} catch (Exception e) {
			throw new RuntimeException("Couldn't initialize compatibility layer", e);
		}

		OFCLogger.debug("Compatibility layer successfully loaded");
	}

	public static boolean isMojangMapped() {
		return instance.isMojangMapped();
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
}
