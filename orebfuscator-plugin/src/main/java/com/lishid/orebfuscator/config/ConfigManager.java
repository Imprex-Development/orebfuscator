/**
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.config;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.lishid.orebfuscator.NmsInstance;
import com.lishid.orebfuscator.utils.Globals;
import com.lishid.orebfuscator.utils.MaterialHelper;

public class ConfigManager {
	private static final int CONFIG_VERSION = 13;

	private JavaPlugin plugin;
	private Logger logger;
	private OrebfuscatorConfig orebfuscatorConfig;
	private MaterialReader materialReader;

	public ConfigManager(JavaPlugin plugin, Logger logger, OrebfuscatorConfig orebfuscatorConfig) {
		this.plugin = plugin;
		this.logger = logger;
		this.orebfuscatorConfig = orebfuscatorConfig;
		this.materialReader = new MaterialReader(this.plugin, this.logger);
	}

	public WorldConfig getWorld(World world) {
		if (world == null) {
			return null;
		}

		WorldConfig baseCfg;

		switch (world.getEnvironment()) {
		case THE_END:
			baseCfg = this.orebfuscatorConfig.getEndWorld();
			break;
		case NETHER:
			baseCfg = this.orebfuscatorConfig.getNetherWorld();
			break;
		default:
			baseCfg = this.orebfuscatorConfig.getNormalWorld();
			break;
		}

		WorldConfig cfg = this.orebfuscatorConfig.getWorld(world.getName());

		if (cfg == null) {
			return baseCfg;
		}

		if (!cfg.isInitialized()) {
			cfg.init(baseCfg);
			this.logger.log(Level.INFO,
					Globals.LogPrefix + "Config for world '" + world.getName() + "' is initialized.");
		}

		return cfg;
	}

	public void load() {
		// Version check
		int version = this.getInt("ConfigVersion", CONFIG_VERSION);
		if (version < CONFIG_VERSION) {
			if (version <= 12) {
				new Convert12To13(this.plugin).convert();
				this.logger.info(Globals.LogPrefix + "Configuration file have been converted to new version.");
			} else {
				this.getConfig().set("ConfigVersion", CONFIG_VERSION);
			}
		}

		boolean useCache = this.getBoolean("Booleans.UseCache", true);
		int maxLoadedCacheFiles = this.getInt("Integers.MaxLoadedCacheFiles", 64, 16, 128);
		String cacheLocation = this.getString("Strings.CacheLocation", "orebfuscator_cache");
		int deleteCacheFilesAfterDays = this.getInt("Integers.DeleteCacheFilesAfterDays", 0);
		boolean enabled = this.getBoolean("Booleans.Enabled", true);
		boolean updateOnDamage = this.getBoolean("Booleans.UpdateOnDamage", true);

		int engineMode = this.getInt("Integers.EngineMode", 2);
		if (engineMode != 1 && engineMode != 2) {
			engineMode = 2;
			this.logger.info(Globals.LogPrefix + "EngineMode must be 1 or 2.");
		}

		int initialRadius = this.getInt("Integers.InitialRadius", 1, 0, 2);
		if (initialRadius == 0) {
			this.logger.info(Globals.LogPrefix
					+ "Warning, InitialRadius is 0. This will cause all exposed blocks to be obfuscated.");
		}

		int updateRadius = this.getInt("Integers.UpdateRadius", 2, 1, 5);
		boolean noObfuscationForMetadata = this.getBoolean("Booleans.NoObfuscationForMetadata", true);
		String noObfuscationForMetadataTagName = this.getString("Strings.NoObfuscationForMetadataTagName", "NPC");
		boolean noObfuscationForOps = this.getBoolean("Booleans.NoObfuscationForOps", false);
		boolean noObfuscationForPermission = this.getBoolean("Booleans.NoObfuscationForPermission", false);
		boolean loginNotification = this.getBoolean("Booleans.LoginNotification", true);
		byte[] transparentBlocks = this.generateTransparentBlocks(engineMode);

		this.orebfuscatorConfig.setUseCache(useCache);
		this.orebfuscatorConfig.setMaxLoadedCacheFiles(maxLoadedCacheFiles);
		this.orebfuscatorConfig.setCacheLocation(cacheLocation);
		this.orebfuscatorConfig.setDeleteCacheFilesAfterDays(deleteCacheFilesAfterDays);
		this.orebfuscatorConfig.setEnabled(enabled);
		this.orebfuscatorConfig.setUpdateOnDamage(updateOnDamage);
		this.orebfuscatorConfig.setEngineMode(engineMode);
		this.orebfuscatorConfig.setInitialRadius(initialRadius);
		this.orebfuscatorConfig.setUpdateRadius(updateRadius);
		this.orebfuscatorConfig.setNoObfuscationForMetadata(noObfuscationForMetadata);
		this.orebfuscatorConfig.setNoObfuscationForMetadataTagName(noObfuscationForMetadataTagName);
		this.orebfuscatorConfig.setNoObfuscationForOps(noObfuscationForOps);
		this.orebfuscatorConfig.setNoObfuscationForPermission(noObfuscationForPermission);
		this.orebfuscatorConfig.setLoginNotification(loginNotification);
		this.orebfuscatorConfig.setTransparentBlocks(transparentBlocks);

		new WorldReader(this.plugin, this.logger, this.orebfuscatorConfig, this.materialReader).load();

		this.orebfuscatorConfig.setProximityHiderEnabled();

		this.logger.info(Globals.LogPrefix + "Proximity Hider is "
				+ (this.orebfuscatorConfig.isProximityHiderEnabled() ? "Enabled" : "Disabled"));

		this.save();
	}

	public void setEngineMode(int value) {
		this.getConfig().set("Integers.EngineMode", value);
		this.save();
		this.orebfuscatorConfig.setEngineMode(value);
	}

	public void setUpdateRadius(int value) {
		this.getConfig().set("Integers.UpdateRadius", value);
		this.save();
		this.orebfuscatorConfig.setUpdateRadius(value);
	}

	public void setInitialRadius(int value) {
		this.getConfig().set("Integers.InitialRadius", value);
		this.save();
		this.orebfuscatorConfig.setInitialRadius(value);
	}

	public void setProximityHiderDistance(int value) {
		this.getConfig().set("Integers.ProximityHiderDistance", value);
		this.save();
		this.orebfuscatorConfig.getDefaultWorld().getProximityHiderConfig().setDistance(value);
	}

	public void setNoObfuscationForOps(boolean value) {
		this.getConfig().set("Booleans.NoObfuscationForOps", value);
		this.save();
		this.orebfuscatorConfig.setNoObfuscationForOps(value);
	}

	public void setNoObfuscationForPermission(boolean value) {
		this.getConfig().set("Booleans.NoObfuscationForPermission", value);
		this.save();
		this.orebfuscatorConfig.setNoObfuscationForPermission(value);
	}

	public void setLoginNotification(boolean value) {
		this.getConfig().set("Booleans.LoginNotification", value);
		this.save();
		this.orebfuscatorConfig.setLoginNotification(value);
	}

	public void setUseCache(boolean value) {
		this.getConfig().set("Booleans.UseCache", value);
		this.save();
		this.orebfuscatorConfig.setUseCache(value);
	}

	public void setEnabled(boolean value) {
		this.getConfig().set("Booleans.Enabled", value);
		this.save();
		this.orebfuscatorConfig.setEnabled(value);
	}

	private FileConfiguration getConfig() {
		return this.plugin.getConfig();
	}

	private void save() {
		this.plugin.saveConfig();
	}

	private String getString(String path, String defaultData, boolean withSave) {
		if (this.getConfig().get(path) == null) {
			if (!withSave) {
				return defaultData;
			}

			this.getConfig().set(path, defaultData);
		}

		return this.getConfig().getString(path, defaultData);
	}

	private String getString(String path, String defaultData) {
		return this.getString(path, defaultData, true);
	}

	private int getInt(String path, int defaultData) {
		return this.getInt(path, defaultData, true);
	}

	private int getInt(String path, int defaultData, boolean withSave) {
		if (this.getConfig().get(path) == null) {
			if (!withSave) {
				return defaultData;
			}

			this.getConfig().set(path, defaultData);
		}

		return this.getConfig().getInt(path, defaultData);
	}

	private int getInt(String path, int defaultData, int min, int max, boolean withSave) {
		if (this.getConfig().get(path) == null && withSave) {
			this.getConfig().set(path, defaultData);
		}

		int value = this.getConfig().get(path) != null ? this.getConfig().getInt(path, defaultData) : defaultData;

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
		if (this.getConfig().get(path) == null) {
			if (!withSave) {
				return defaultData;
			}

			this.getConfig().set(path, defaultData);
		}

		return this.getConfig().getBoolean(path, defaultData);
	}

	private boolean getBoolean(String path, boolean defaultData) {
		return this.getBoolean(path, defaultData, true);
	}

	private byte[] generateTransparentBlocks(int engineMode) {
		byte[] transparentBlocks = new byte[MaterialHelper.getMaxId() + 1];

		for (Material material : Material.values()) {
			if (material.isBlock() && !material.isOccluding()) {
				Set<Integer> ids = NmsInstance.current.getMaterialIds(material);

				for (int id : ids) {
					transparentBlocks[id] = 1;
				}
			}
		}

		Set<Integer> lavaIds = NmsInstance.current.getMaterialIds(Material.LAVA);

		for (int id : lavaIds) {
			transparentBlocks[id] = (byte) (engineMode == 1 ? 0 : 1);
		}

		return transparentBlocks;
	}
}