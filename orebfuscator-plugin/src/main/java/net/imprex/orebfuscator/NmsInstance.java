package net.imprex.orebfuscator;

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
			throw new IllegalStateException("NMS protocol version was already initialized!");
		}

		OFCLogger.info("Searching NMS protocol for server version \"" + MinecraftVersion.getNmsVersion() + "\"!");

		switch (MinecraftVersion.getNmsVersion()) {
		case "v1_16_R2":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_16_R2.NmsManager(config);
			break;

		case "v1_16_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_16_R1.NmsManager(config);
			break;

		case "v1_15_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_15_R1.NmsManager(config);
			break;

		case "v1_14_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_14_R1.NmsManager(config);
			break;

		case "v1_13_R2":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_13_R2.NmsManager(config);
			break;

		case "v1_13_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_13_R1.NmsManager(config);
			break;

		case "v1_12_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_12_R1.NmsManager(config);
			break;

		case "v1_11_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_11_R1.NmsManager(config);
			break;

		case "v1_10_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_10_R1.NmsManager(config);
			break;

		case "v1_9_R2":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_9_R2.NmsManager(config);
			break;
		}

		if (NmsInstance.instance != null) {
			OFCLogger.info("NMS protocol for server version \"" + MinecraftVersion.getNmsVersion() + "\" found!");
		} else {
			throw new RuntimeException("Server version \"" + MinecraftVersion.getNmsVersion() + "\" is currently not supported!");
		}
	}

	public static AbstractRegionFileCache<?> getRegionFileCache() {
		return instance.getRegionFileCache();
	}

	public static int getBitsPerBlock() {
		return instance.getBitsPerBlock();
	}

	public static int getMaterialSize() {
		return instance.getMaterialSize();
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

	public static int getCaveAirBlockId() {
		return instance.getCaveAirBlockId();
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

	public static boolean canApplyPhysics(Material material) {
		return instance.canApplyPhysics(material);
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