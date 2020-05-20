package net.imprex.orebfuscator;

import com.lishid.orebfuscator.utils.Globals;

import net.imprex.orebfuscator.chunk.ChunkCapabilities;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.NmsManager;
import net.imprex.orebfuscator.util.OFCLogger;

public class NmsInstance {

	private static NmsManager instance;

	public static void initialize(Config config) {
		if (NmsInstance.instance != null) {
			throw new IllegalStateException("NMS protocol version was already initialized!");
		}

		OFCLogger.log("Searching NMS protocol for server version \"" + Globals.SERVER_VERSION + "\"!");

		// hasLight < 1.14
		// hasDirectPaletteZeroLength < 1.13
		// hasBlockCount >= 1.14

		switch (Globals.SERVER_VERSION) {
		case "v1_15_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_15_R1.NmsManager(config);
			ChunkCapabilities.hasBlockCount();
			break;

		case "v1_14_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_14_R1.NmsManager(config);
			ChunkCapabilities.hasBlockCount();
			break;

		case "v1_13_R2":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_13_R2.NmsManager(config);
			ChunkCapabilities.hasLightArray();
			break;

		case "v1_13_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_13_R1.NmsManager(config);
			ChunkCapabilities.hasLightArray();
			break;

		case "v1_12_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_12_R1.NmsManager(config);
			ChunkCapabilities.hasLightArray();
			ChunkCapabilities.hasDirectPaletteZeroLength();
			break;

		case "v1_11_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_11_R1.NmsManager(config);
			ChunkCapabilities.hasLightArray();
			ChunkCapabilities.hasDirectPaletteZeroLength();
			break;

		case "v1_10_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_10_R1.NmsManager(config);
			ChunkCapabilities.hasLightArray();
			ChunkCapabilities.hasDirectPaletteZeroLength();
			break;

		case "v1_9_R2":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_9_R2.NmsManager(config);
			ChunkCapabilities.hasLightArray();
			ChunkCapabilities.hasDirectPaletteZeroLength();
			break;
		}

		if (NmsInstance.instance != null) {
			OFCLogger.log("NMS protocol for server version \"" + Globals.SERVER_VERSION + "\" found!");
		} else {
			throw new RuntimeException("Server version \"" + Globals.SERVER_VERSION + "\" is currently not supported!");
		}
	}

	public static NmsManager get() {
		if (NmsInstance.instance == null) {
			throw new IllegalStateException("No NmsManager instance initialized");
		}
		return NmsInstance.instance;
	}

	public static void close() {
		if (NmsInstance.instance != null) {
			NmsInstance.instance.close();
			NmsInstance.instance = null;
		}
	}
}