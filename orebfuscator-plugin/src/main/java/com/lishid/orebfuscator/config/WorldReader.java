/**
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import com.lishid.orebfuscator.utils.Globals;

public class WorldReader {
	private Map<String, WorldConfig> worlds;

	private JavaPlugin plugin;
	private Logger logger;
	private OrebfuscatorConfig orebfuscatorConfig;
	private MaterialReader materialReader;

	public WorldReader(JavaPlugin plugin, Logger logger, OrebfuscatorConfig orebfuscatorConfig,
			MaterialReader materialReader) {
		this.plugin = plugin;
		this.logger = logger;
		this.orebfuscatorConfig = orebfuscatorConfig;
		this.materialReader = materialReader;
	}

	public void load() {
		ConfigurationSection section = this.getConfig().getConfigurationSection("Worlds");
		Set<String> keys = section != null ? section.getKeys(false) : new HashSet<>();

		this.worlds = new HashMap<>();

		for (String worldName : keys) {
			if (!this.worlds.containsKey(worldName)) {
				try {
					WorldConfig world = new WorldConfig();
					String path = "Worlds." + worldName;

					Boolean enabled = this.getBoolean(path + ".Enabled", world.isEnabled(), false);
					Boolean antiTexturePackAndFreecam = this.getBoolean(path + ".AntiTexturePackAndFreecam",
							world.isAntiTexturePackAndFreecam(), false);
					Integer airGeneratorMaxChance = this.getInt(path + ".AirGeneratorMaxChance",
							world.getAirGeneratorMaxChance(), 40, 100, false);
					Boolean darknessHideBlocks = this.getBoolean(path + ".DarknessHideBlocks", world.isDarknessHideBlocks(),
							false);
					Boolean bypassObfuscationForSignsWithText = this.getBoolean(path + ".BypassObfuscationForSignsWithText",
							world.isBypassObfuscationForSignsWithText(), false);
					HashSet<Integer> darknessBlocks = this.readBlockMatrix(world.getDarknessBlocks(), path + ".DarknessBlocks",
							false);
					Integer mode1Block = this.materialReader.getMaterialIdByPath(path + ".Mode1Block", world.getMode1BlockId(),
							false);
					Integer[] randomBlocks = this.materialReader.getMaterialIdsByPath(path + ".RandomBlocks",
							world.getRandomBlocks(), false);
					HashSet<Integer> obfuscateBlocks = this.readBlockMatrix(world.getObfuscateBlocks(),
							path + ".ObfuscateBlocks", false);

					this.readProximityHider(path + ".ProximityHider", world, false);

					world.setEnabled(enabled);
					world.setAntiTexturePackAndFreecam(antiTexturePackAndFreecam);
					world.setBypassObfuscationForSignsWithText(bypassObfuscationForSignsWithText);
					world.setAirGeneratorMaxChance(airGeneratorMaxChance);
					world.setDarknessHideBlocks(darknessHideBlocks);
					world.setDarknessBlocks(darknessBlocks);
					world.setMode1BlockId(mode1Block);
					world.setRandomBlocks(randomBlocks);
					world.setObfuscateBlocks(obfuscateBlocks);
					world.setName(worldName);

					if (world.getProximityHiderConfig().isEnabled()) {
						world.setPaletteBlocks();
						world.setObfuscateAndProximityBlocks();
					}

					this.worlds.put(worldName, world);

					this.logger.log(Level.INFO, Globals.LogPrefix + "World name '" + worldName + "' has been loaded.");
				} catch(Exception e) {
					this.logger.log(Level.WARNING, Globals.LogPrefix + "World name '" + worldName + "' couldn't been loaded.");
					e.printStackTrace();
				}
			}
		}

		this.orebfuscatorConfig.setWorlds(this.worlds);
	}

	private FileConfiguration getConfig() {
		return this.plugin.getConfig();
	}

	private void readProximityHider(String path, WorldConfig worldConfig, boolean withSave) {
		ProximityHiderConfig cfg = worldConfig.getProximityHiderConfig();
		Integer[] defaultProximityHiderBlockIds = cfg.getProximityHiderBlocks() != null
				? cfg.getProximityHiderBlocks().toArray(new Integer[0])
				: null;

		Boolean enabled = this.getBoolean(path + ".Enabled", cfg.isEnabled(), withSave);
		Integer distance = this.getInt(path + ".Distance", cfg.getDistance(), 2, 64, withSave);
		Integer specialBlockID = this.materialReader.getMaterialIdByPath(path + ".SpecialBlock",
				cfg.getSpecialBlockID(), withSave);
		Integer y = this.getInt(path + ".Y", cfg.getY(), 0, 255, withSave);
		Boolean useSpecialBlock = this.getBoolean(path + ".UseSpecialBlock", cfg.isUseSpecialBlock(), withSave);
		Boolean useYLocationProximity = this.getBoolean(path + ".ObfuscateAboveY", cfg.isObfuscateAboveY(),
				withSave);
		Integer[] proximityHiderBlockIds = this.materialReader
				.getMaterialIdsByPath(path + ".ProximityHiderBlocks", defaultProximityHiderBlockIds, withSave);
		ProximityHiderConfig.BlockSetting[] proximityHiderBlockSettings = this.readProximityHiderBlockSettings(
				path + ".ProximityHiderBlockSettings", cfg.getProximityHiderBlockSettings());
		Boolean useFastGazeCheck = this.getBoolean(path + ".UseFastGazeCheck", cfg.isUseFastGazeCheck(),
				withSave);

		cfg.setEnabled(enabled);
		cfg.setDistance(distance);
		cfg.setSpecialBlockID(specialBlockID);
		cfg.setY(y);
		cfg.setUseSpecialBlock(useSpecialBlock);
		cfg.setObfuscateAboveY(useYLocationProximity);
		cfg.setProximityHiderBlocks(proximityHiderBlockIds);
		cfg.setProximityHiderBlockSettings(proximityHiderBlockSettings);
		cfg.setUseFastGazeCheck(useFastGazeCheck);

		cfg.setProximityHiderBlockMatrix();
	}

	private ProximityHiderConfig.BlockSetting[] readProximityHiderBlockSettings(String configKey,
			ProximityHiderConfig.BlockSetting[] defaultBlocks) {
		ConfigurationSection section = this.getConfig().getConfigurationSection(configKey);

		if (section == null) {
			return defaultBlocks;
		}

		Set<String> keys = section.getKeys(false);

		List<ProximityHiderConfig.BlockSetting> list = new ArrayList<>();

		for (String key : keys) {
			Set<Integer> blockIds = this.materialReader.getMaterialIds(key);

			if (blockIds == null) {
				continue;
			}

			String blockPath = configKey + "." + key;
			int blockY = this.getConfig().getInt(blockPath + ".Y", 255);
			boolean blockObfuscateAboveY = this.getConfig().getBoolean(blockPath + ".ObfuscateAboveY", false);

			for (int blockId : blockIds) {
				ProximityHiderConfig.BlockSetting block = new ProximityHiderConfig.BlockSetting();
				block.blockId = blockId;
				block.y = blockY;
				block.obfuscateAboveY = blockObfuscateAboveY;

				list.add(block);
			}
		}

		return list.toArray(new ProximityHiderConfig.BlockSetting[0]);
	}

	private HashSet<Integer> readBlockMatrix(HashSet<Integer> original, String configKey, boolean withSave) {
		Integer[] defaultBlockIds;

		if (original != null && original.size() != 0) {
			defaultBlockIds = new Integer[original.size()];

			int index = 0;

			for (Integer id : original) {
				defaultBlockIds[index++] = id;
			}
		} else {
			defaultBlockIds = null;
		}

		Integer[] blockIds = this.materialReader.getMaterialIdsByPath(configKey, defaultBlockIds, withSave);

		HashSet<Integer> blocks;

		if (blockIds != null) {
			blocks = new HashSet<>();

			for (int id : blockIds) {
				blocks.add(id);
			}
		} else {
			blocks = original;
		}

		return blocks;
	}

	private Integer getInt(String path, Integer defaultData, int min, int max, boolean withSave) {
		if (this.getConfig().get(path) == null && withSave) {
			this.getConfig().set(path, defaultData);
		}

		Integer value = this.getConfig().get(path) != null ? (Integer) this.getConfig().getInt(path) : defaultData;

		if (value != null) {
			if (value < min) {
				value = min;
			} else if (value > max) {
				value = max;
			}
		}

		return value;
	}

	private Boolean getBoolean(String path, Boolean defaultData, boolean withSave) {
		if (this.getConfig().get(path) == null) {
			if (!withSave) {
				return defaultData;
			}

			this.getConfig().set(path, defaultData);
		}

		return this.getConfig().get(path) != null ? (Boolean) this.getConfig().getBoolean(path) : defaultData;
	}
}