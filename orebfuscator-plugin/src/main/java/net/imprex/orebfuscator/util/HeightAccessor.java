package net.imprex.orebfuscator.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;

import dev.imprex.orebfuscator.logging.OfcLogger;
import net.imprex.orebfuscator.chunk.ChunkCapabilities;

public class HeightAccessor {

	private static final Map<World, HeightAccessor> ACCESSOR_LOOKUP = new ConcurrentHashMap<>();

	public static HeightAccessor get(World world) {
		return ACCESSOR_LOOKUP.computeIfAbsent(world, HeightAccessor::new);
	}

	private static final MethodAccessor WORLD_GET_MAX_HEIGHT = getWorldMethod("getMaxHeight");
	private static final MethodAccessor WORLD_GET_MIN_HEIGHT = getWorldMethod("getMinHeight");

	private static MethodAccessor getWorldMethod(String methodName) {
		if (ChunkCapabilities.hasDynamicHeight()) {
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

	public static void registerListener(Plugin plugin) {
		Bukkit.getPluginManager().registerEvents(new Listener() {
			@EventHandler
			public void onWorldUnload(WorldUnloadEvent event) {
				ACCESSOR_LOOKUP.remove(event.getWorld());
			}
		}, plugin);
	}

	private final String worldName;

	private final int maxHeight;
	private final int minHeight;

	private HeightAccessor(World world) {
		this.worldName = world.getName();

		if (ChunkCapabilities.hasDynamicHeight()) {
			this.maxHeight = (int) WORLD_GET_MAX_HEIGHT.invoke(world);
			this.minHeight = (int) WORLD_GET_MIN_HEIGHT.invoke(world);
		} else {
			this.maxHeight = 256;
			this.minHeight = 0;
		}
	}

	public int getHeight() {
		return this.maxHeight - this.minHeight;
	}

	public int getMinBuildHeight() {
		return this.minHeight;
	}

	public int getMaxBuildHeight() {
		return this.maxHeight;
	}

	public int getSectionCount() {
		return this.getMaxSection() - this.getMinSection();
	}

	public int getMinSection() {
		return blockToSectionCoord(this.getMinBuildHeight());
	}

	public int getMaxSection() {
		return blockToSectionCoord(this.getMaxBuildHeight() - 1) + 1;
	}

	public int getSectionIndex(int y) {
		return blockToSectionCoord(y) - getMinSection();
	}

	@Override
	public String toString() {
		return String.format("[%s, minY=%s, maxY=%s]", worldName, minHeight, maxHeight);
	}
}
