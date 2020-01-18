/**
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.lishid.orebfuscator.Orebfuscator;
import com.lishid.orebfuscator.utils.Globals;
import com.lishid.orebfuscator.utils.MaterialHelper;

import net.imprex.orebfuscator.NmsInstance;

public class ConfigManager {

	private static final int CONFIG_VERSION = 13;

	private final JavaPlugin plugin;
	private final MaterialReader materialReader;

	private OrebfuscatorConfig orebfuscatorConfig;

	public ConfigManager(JavaPlugin plugin) {
		this.plugin = plugin;

		this.materialReader = new MaterialReader(this.plugin);
	}

	public WorldConfig getWorld(World world) {
		if (world == null) {
			return null;
		}

		return this.orebfuscatorConfig.getWorld(world.getName());
	}

	public void initialize() {
		this.createConfigIfNotExist();
		this.load();
	}

	public void postInitialize() {
		this.orebfuscatorConfig.setTransparentBlocks(this.generateTransparentBlocks());

		new WorldReader(this.plugin, Orebfuscator.LOGGER, this.orebfuscatorConfig, this.materialReader).load();

		this.orebfuscatorConfig.setProximityHiderEnabled();

		Orebfuscator.LOGGER.info(Globals.LOG_PREFIX + "Proximity Hider is "
				+ (this.orebfuscatorConfig.isProximityHiderEnabled() ? "Enabled" : "Disabled"));
	}

	public void load() {
		if (this.orebfuscatorConfig == null) {
			this.orebfuscatorConfig = new OrebfuscatorConfig();
		}

		// Version check
		if (this.getInt("ConfigVersion", CONFIG_VERSION) < CONFIG_VERSION) {
			Orebfuscator.LOGGER.info(Globals.LOG_PREFIX + "Current config is not up to date, please delete your config");
			throw new RuntimeException("Current config is not up to date, please delete your config");
		}

		boolean enabled = this.getBoolean("Booleans.Enabled", true);
		boolean updateOnDamage = this.getBoolean("Booleans.UpdateOnDamage", true);

		int engineMode = this.getInt("Integers.EngineMode", 2);
		if (engineMode != 1 && engineMode != 2) {
			engineMode = 2;
			Orebfuscator.LOGGER.info(Globals.LOG_PREFIX + "EngineMode must be 1 or 2.");
		}

		int initialRadius = this.getInt("Integers.InitialRadius", 1, 0, 2);
		if (initialRadius == 0) {
			Orebfuscator.LOGGER.info(Globals.LOG_PREFIX
					+ "Warning, InitialRadius is 0. This will cause all exposed blocks to be obfuscated.");
		}

		int updateRadius = this.getInt("Integers.UpdateRadius", 2, 1, 5);
		boolean noObfuscationForMetadata = this.getBoolean("Booleans.NoObfuscationForMetadata", true);
		String noObfuscationForMetadataTagName = this.getString("Strings.NoObfuscationForMetadataTagName", "NPC");
		boolean noObfuscationForOps = this.getBoolean("Booleans.NoObfuscationForOps", false);
		boolean noObfuscationForPermission = this.getBoolean("Booleans.NoObfuscationForPermission", false);
		boolean loginNotification = this.getBoolean("Booleans.LoginNotification", true);

		this.orebfuscatorConfig.setEnabled(enabled);
		this.orebfuscatorConfig.setUpdateOnDamage(updateOnDamage);
		this.orebfuscatorConfig.setInitialRadius(initialRadius);
		this.orebfuscatorConfig.setUpdateRadius(updateRadius);
		this.orebfuscatorConfig.setNoObfuscationForMetadata(noObfuscationForMetadata);
		this.orebfuscatorConfig.setNoObfuscationForMetadataTagName(noObfuscationForMetadataTagName);
		this.orebfuscatorConfig.setNoObfuscationForOps(noObfuscationForOps);
		this.orebfuscatorConfig.setNoObfuscationForPermission(noObfuscationForPermission);
		this.orebfuscatorConfig.setLoginNotification(loginNotification);

		this.save();
	}

	public void reload() {
		this.createConfigIfNotExist();

		this.plugin.reloadConfig();

		this.load();
	}

	public void createConfigIfNotExist() {
		Path path = this.plugin.getDataFolder().toPath().resolve("config.yml");

		if (Files.notExists(path)) {
			try {
				Matcher matcher = Globals.NMS_PATTERN.matcher(Globals.SERVER_VERSION);

				if (!matcher.find()) {
					throw new RuntimeException("WTF is this version!?");
				}

				String configVersion = matcher.group(1) + "." + matcher.group(2);

				if (Files.notExists(path.getParent())) {
					Files.createDirectories(path.getParent());
				}

				Files.copy(Orebfuscator.class.getResourceAsStream("/resources/config-" + configVersion + ".yml"), path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void setUpdateRadius(int value) {
		this.getPluginConfig().set("Integers.UpdateRadius", value);
		this.save();
		this.orebfuscatorConfig.setUpdateRadius(value);
	}

	public void setInitialRadius(int value) {
		this.getPluginConfig().set("Integers.InitialRadius", value);
		this.save();
		this.orebfuscatorConfig.setInitialRadius(value);
	}

	public void setNoObfuscationForOps(boolean value) {
		this.getPluginConfig().set("Booleans.NoObfuscationForOps", value);
		this.save();
		this.orebfuscatorConfig.setNoObfuscationForOps(value);
	}

	public void setNoObfuscationForPermission(boolean value) {
		this.getPluginConfig().set("Booleans.NoObfuscationForPermission", value);
		this.save();
		this.orebfuscatorConfig.setNoObfuscationForPermission(value);
	}

	public void setLoginNotification(boolean value) {
		this.getPluginConfig().set("Booleans.LoginNotification", value);
		this.save();
		this.orebfuscatorConfig.setLoginNotification(value);
	}

	public void setEnabled(boolean value) {
		this.getPluginConfig().set("Booleans.Enabled", value);
		this.save();
		this.orebfuscatorConfig.setEnabled(value);
	}

	private FileConfiguration getPluginConfig() {
		return this.plugin.getConfig();
	}

	private void save() {
		this.plugin.saveConfig();
	}

	private String getString(String path, String defaultData, boolean withSave) {
		if (this.getPluginConfig().get(path) == null) {
			if (!withSave) {
				return defaultData;
			}

			this.getPluginConfig().set(path, defaultData);
		}

		return this.getPluginConfig().getString(path, defaultData);
	}

	private String getString(String path, String defaultData) {
		return this.getString(path, defaultData, true);
	}

	private int getInt(String path, int defaultData) {
		return this.getInt(path, defaultData, true);
	}

	private int getInt(String path, int defaultData, boolean withSave) {
		if (this.getPluginConfig().get(path) == null) {
			if (!withSave) {
				return defaultData;
			}

			this.getPluginConfig().set(path, defaultData);
		}

		return this.getPluginConfig().getInt(path, defaultData);
	}

	private int getInt(String path, int defaultData, int min, int max, boolean withSave) {
		if (this.getPluginConfig().get(path) == null && withSave) {
			this.getPluginConfig().set(path, defaultData);
		}

		int value = this.getPluginConfig().get(path) != null ? this.getPluginConfig().getInt(path, defaultData) : defaultData;

		if (value < min) {
			value = min;
		} else if (value > max) {
			value = max;
		}

		return value;
	}

	private int getInt(String path, int defaultData, int min, int max) {
		return this.getInt(path, defaultData, min, max, true);
	}

	private boolean getBoolean(String path, boolean defaultData, boolean withSave) {
		if (this.getPluginConfig().get(path) == null) {
			if (!withSave) {
				return defaultData;
			}

			this.getPluginConfig().set(path, defaultData);
		}

		return this.getPluginConfig().getBoolean(path, defaultData);
	}

	private boolean getBoolean(String path, boolean defaultData) {
		return this.getBoolean(path, defaultData, true);
	}

	private byte[] generateTransparentBlocks() {
		byte[] transparentBlocks = new byte[MaterialHelper.getMaxId() + 1];

		for (Material material : Material.values()) {
			if (material.isBlock() && !material.isOccluding()) {
				Set<Integer> ids = NmsInstance.get().getMaterialIds(material);

				for (int id : ids) {
					transparentBlocks[id] = 1;
				}
			}
		}

//		Set<Integer> lavaIds = NmsInstance.get().getMaterialIds(Material.LAVA);
//
//		for (int id : lavaIds) {
//			transparentBlocks[id] = (byte) (engineMode == 1 ? 0 : 1);
//		}

		return transparentBlocks;
	}

	public OrebfuscatorConfig getConfig() {
		return this.orebfuscatorConfig;
	}
}