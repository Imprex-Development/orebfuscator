package net.imprex.orebfuscator;

import com.lishid.orebfuscator.Orebfuscator;
import com.lishid.orebfuscator.config.OrebfuscatorConfig;
import com.lishid.orebfuscator.nms.INmsManager;
import com.lishid.orebfuscator.utils.Globals;

public class NmsInstance {

	private static INmsManager instance;

	public static void initialize(OrebfuscatorConfig config) {
		if (NmsInstance.instance != null) {
			throw new IllegalStateException("NMS protocol version was already initialized!");
		}

		Orebfuscator.log("Searching NMS protocol for server version \"" + Globals.SERVER_VERSION + "\"!");

		switch (Globals.SERVER_VERSION) {
		case "v1_15_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_15_R1.NmsManager(config.getCacheConfig());
			break;

		case "v1_14_R1":
			NmsInstance.instance = new net.imprex.orebfuscator.nms.v1_14_R1.NmsManager(config.getCacheConfig());
			break;

		case "v1_13_R2":
			NmsInstance.instance = new com.lishid.orebfuscator.nms.v1_13_R2.NmsManager(config.getCacheConfig());
			break;

		case "v1_13_R1":
			NmsInstance.instance = new com.lishid.orebfuscator.nms.v1_13_R1.NmsManager(config.getCacheConfig());
			break;

		case "v1_12_R1":
			NmsInstance.instance = new com.lishid.orebfuscator.nms.v1_12_R1.NmsManager(config.getCacheConfig());
			break;

		case "v1_11_R1":
			NmsInstance.instance = new com.lishid.orebfuscator.nms.v1_11_R1.NmsManager(config.getCacheConfig());
			break;

		case "v1_10_R1":
			NmsInstance.instance = new com.lishid.orebfuscator.nms.v1_10_R1.NmsManager(config.getCacheConfig());
			break;

		case "v1_9_R2":
			NmsInstance.instance = new com.lishid.orebfuscator.nms.v1_9_R2.NmsManager(config.getCacheConfig());
			break;

		case "v1_9_R1":
			NmsInstance.instance = new com.lishid.orebfuscator.nms.v1_9_R1.NmsManager(config.getCacheConfig());
			break;
		}

		if (NmsInstance.instance != null) {
			Orebfuscator.log("NMS protocol for server version \"" + Globals.SERVER_VERSION + "\" found!");
		} else {
			throw new RuntimeException("Server version \"" + Globals.SERVER_VERSION + "\" is currently not supported!");
		}
	}

	public static INmsManager get() {
		if (NmsInstance.instance == null) {
			throw new IllegalStateException("No NmsManager instance initialized");
		}
		return NmsInstance.instance;
	}
}