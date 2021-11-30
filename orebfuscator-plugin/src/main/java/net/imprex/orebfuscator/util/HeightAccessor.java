package net.imprex.orebfuscator.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.World;

import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.MethodAccessor;

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
			OFCLogger.info("HeightAccessor found method: World::" + methodName + "()");
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

	public static void thisMethodIsUsedToInitializeStaticFields() {
	}

	private final int maxHeight;
	private final int minHeight;

	private HeightAccessor(World world) {
		if (ChunkCapabilities.hasDynamicHeight()) {
			this.maxHeight = (int) WORLD_GET_MAX_HEIGHT.invoke(world);
			this.minHeight = (int) WORLD_GET_MIN_HEIGHT.invoke(world);
		} else {
			this.maxHeight = 256;
			this.minHeight = 0;
		}
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
		return SectionPosition.blockToSectionCoord(this.getMinBuildHeight());
	}

	public int getMaxSection() {
		return SectionPosition.blockToSectionCoord(this.getMaxBuildHeight() - 1) + 1;
	}

	public int getSectionIndex(int y) {
		return SectionPosition.blockToSectionCoord(y) - getMinSection();
	}
}
