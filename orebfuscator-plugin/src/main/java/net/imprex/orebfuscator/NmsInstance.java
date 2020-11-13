package net.imprex.orebfuscator;

import java.lang.reflect.Constructor;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.nms.BlockStateHolder;
import net.imprex.orebfuscator.nms.NmsManager;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.MinecraftVersion;
import net.imprex.orebfuscator.util.OFCLogger;

public class NmsInstance {

	private static NmsManager instance;

	public static void initialize(Config config) {
		if (NmsInstance.instance != null) {
			throw new IllegalStateException("NMS adapter is already initialized!");
		}

		String nmsVersion = MinecraftVersion.getNmsVersion();
		OFCLogger.info("Searching NMS adapter for server version \"" + nmsVersion + "\"!");

		try {
			String className = "net.imprex.orebfuscator.nms." + nmsVersion + ".NmsManager";
			Class<? extends NmsManager> nmsManager = Class.forName(className).asSubclass(NmsManager.class);
			Constructor<? extends NmsManager> constructor = nmsManager.getConstructor(Config.class);
			NmsInstance.instance = constructor.newInstance(config);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Server version \"" + nmsVersion + "\" is currently not supported!");
		} catch (Exception e) {
			throw new RuntimeException("Couldn't initialize NMS adapter", e);
		}

		OFCLogger.info("NMS adapter for server version \"" + nmsVersion + "\" found!");
	}

	public static AbstractRegionFileCache<?> getRegionFileCache() {
		return instance.getRegionFileCache();
	}

	public static int getBitsPerBlock() {
		return instance.getBitsPerBlock();
	}

	public static int getMaterialSize() {
		return instance.getTotalBlockCount();
	}

	public static Optional<Material> getMaterialByName(String name) {
		return instance.getMaterialByName(name);
	}

	public static Optional<String> getNameByMaterial(Material material) {
		return instance.getNameByMaterial(material);
	}

	public static Set<Integer> getMaterialIds(Material material) {
		return instance.getMaterialIds(material);
	}

	public static boolean isHoe(Material material) {
		return instance.isHoe(material);
	}

	public static boolean isAir(int blockId) {
		return instance.isAir(blockId);
	}

	public static boolean isTileEntity(int blockId) {
		return instance.isTileEntity(blockId);
	}

	public static BlockStateHolder getBlockState(World world, int x, int y, int z) {
		return instance.getBlockState(world, x, y, z);
	}

	public static int loadChunkAndGetBlockId(World world, int x, int y, int z) {
		return instance.loadChunkAndGetBlockId(world, x, y, z);
	}

	public static boolean sendBlockChange(Player player, BlockPos blockCoords) {
		return instance.sendBlockChange(player, blockCoords);
	}

	public static void close() {
		if (instance != null) {
			instance.close();
			instance = null;
		}
	}
}