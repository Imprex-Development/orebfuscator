package net.imprex.orebfuscator.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.plugin.Plugin;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.util.MinecraftVersion;
import net.imprex.orebfuscator.util.OFCLogger;

public class OrebfuscatorConfig implements Config {

	private static final int CONFIG_VERSION = 1;

	private final OrebfuscatorGeneralConfig generalConfig = new OrebfuscatorGeneralConfig();
	private final OrebfuscatorCacheConfig cacheConfig = new OrebfuscatorCacheConfig();

	private final List<OrebfuscatorWorldConfig> world = new ArrayList<>();
	private final List<OrebfuscatorProximityConfig> proximityWorlds = new ArrayList<>();

	private final Map<World, OrebfuscatorConfig.WorldEntry> worldToEntry = new WeakHashMap<>();

	private final Plugin plugin;

	private byte[] hash;

	public OrebfuscatorConfig(Plugin plugin) {
		this.plugin = plugin;

		this.reload();
	}

	public void reload() {
		this.createConfigIfNotExist();
		this.plugin.reloadConfig();

		this.serialize(this.plugin.getConfig());
		this.initialize();
	}

	public void store() {
		this.createConfigIfNotExist();

		ConfigurationSection section = this.plugin.getConfig();
		for (String path : section.getKeys(false)) {
			section.set(path, null);
		}
		this.deserialize(section);
		this.plugin.saveConfig();
	}

	private void createConfigIfNotExist() {
		Path dataFolder = this.plugin.getDataFolder().toPath();
		Path path = dataFolder.resolve("config.yml");

		if (Files.notExists(path)) {
			try {
				String configVersion = MinecraftVersion.getMajorVersion() + "." + MinecraftVersion.getMinorVersion();

				if (Files.notExists(dataFolder)) {
					Files.createDirectories(dataFolder);
				}

				Files.copy(Orebfuscator.class.getResourceAsStream("/resources/config-" + configVersion + ".yml"), path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		this.hash = this.calculateHash(path);
	}

	private byte[] calculateHash(Path path) {
		try {
			MessageDigest md5Digest = MessageDigest.getInstance("MD5");
			md5Digest.update(MinecraftVersion.getNmsVersion().getBytes(StandardCharsets.UTF_8));
			return md5Digest.digest(Files.readAllBytes(path));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return new byte[0];
	}

	private void serialize(ConfigurationSection section) {
		if (section.getInt("version", -1) != CONFIG_VERSION) {
			throw new RuntimeException("config is not up to date, please delete your config");
		}

		this.world.clear();
		this.proximityWorlds.clear();

		ConfigurationSection generalSection = section.getConfigurationSection("general");
		if (generalSection != null) {
			this.generalConfig.serialize(generalSection);
		} else {
			OFCLogger.warn("config section 'general' is missing, using default one");
		}

		ConfigurationSection cacheSection = section.getConfigurationSection("cache");
		if (cacheSection != null) {
			this.cacheConfig.serialize(cacheSection);
		} else {
			OFCLogger.warn("config section 'cache' is missing, using default one");
		}

		NmsInstance.close();
		NmsInstance.initialize(this);

		List<ConfigurationSection> worldSectionList = ConfigParser.serializeSectionList(section, "world");
		if (!worldSectionList.isEmpty()) {
			for (ConfigurationSection worldSection : worldSectionList) {
				OrebfuscatorWorldConfig worldConfig = new OrebfuscatorWorldConfig();
				worldConfig.serialize(worldSection);
				this.world.add(worldConfig);
			}
		} else {
			OFCLogger.warn("config section 'world' is missing or empty");
		}

		List<ConfigurationSection> proximitySectionList = ConfigParser.serializeSectionList(section, "proximity");
		if (!proximitySectionList.isEmpty()) {
			for (ConfigurationSection proximitySection : proximitySectionList) {
				OrebfuscatorProximityConfig proximityHiderConfig = new OrebfuscatorProximityConfig();
				proximityHiderConfig.serialize(proximitySection);
				this.proximityWorlds.add(proximityHiderConfig);
			}
		} else {
			OFCLogger.warn("config section 'proximity' is missing or empty");
		}
	}

	private void deserialize(ConfigurationSection section) {
		section.set("version", CONFIG_VERSION);

		this.generalConfig.deserialize(section.createSection("general"));
		this.cacheConfig.deserialize(section.createSection("cache"));

		List<ConfigurationSection> worldSectionList = new ArrayList<>();
		for (OrebfuscatorWorldConfig worldConfig : this.world) {
			ConfigurationSection worldSection = new MemoryConfiguration();
			worldConfig.deserialize(worldSection);
			worldSectionList.add(worldSection);
		}
		section.set("world", worldSectionList);

		List<ConfigurationSection> proximitySectionList = new ArrayList<>();
		for (OrebfuscatorProximityConfig proximityConfig : this.proximityWorlds) {
			ConfigurationSection proximitySection = new MemoryConfiguration();
			proximityConfig.deserialize(proximitySection);
			proximitySectionList.add(proximitySection);
		}
		section.set("proximity", proximitySectionList);
	}

	private void initialize() {
		this.worldToEntry.clear();

		Set<String> worldNames = new HashSet<>();
		for (OrebfuscatorWorldConfig worldConfig : this.world) {
			worldConfig.initialize();
			for (String worldName : worldConfig.worlds()) {
				if (worldNames.contains(worldName)) {
					OFCLogger.warn("world " + worldName + " has more than one world config choosing first one");
				} else {
					worldNames.add(worldName);
				}
			}
		}

		worldNames.clear();
		for (OrebfuscatorProximityConfig proximityConfig : this.proximityWorlds) {
			proximityConfig.initialize();
			for (String worldName : proximityConfig.worlds()) {
				if (worldNames.contains(worldName)) {
					OFCLogger.warn("world " + worldName + " has more than one proximity config choosing first one");
				} else {
					worldNames.add(worldName);
				}
			}
		}

		for (World world : Bukkit.getWorlds()) {
			this.worldToEntry.put(world, new WorldEntry(world));
		}
	}

	private WorldEntry getWorldEntry(World world) {
		WorldEntry worldEntry = this.worldToEntry.get(Objects.requireNonNull(world));
		if (worldEntry == null) {
			worldEntry = new WorldEntry(world);
			this.worldToEntry.put(world, worldEntry);
		}
		return worldEntry;
	}

	@Override
	public GeneralConfig general() {
		return this.generalConfig;
	}

	@Override
	public CacheConfig cache() {
		return this.cacheConfig;
	}

	@Override
	public BlockMask blockMask(World world) {
		return this.getWorldEntry(world).blockMask;
	}

	@Override
	public boolean needsObfuscation(World world) {
		WorldEntry worldEntry = this.getWorldEntry(world);
		WorldConfig worldConfig = worldEntry.worldConfig;
		ProximityConfig proximityConfig = worldEntry.proximityConfig;
		return worldConfig != null && worldConfig.enabled() || proximityConfig != null && proximityConfig.enabled();
	}

	@Override
	public OrebfuscatorWorldConfig world(World world) {
		return this.getWorldEntry(world).worldConfig;
	}

	@Override
	public boolean proximityEnabled() {
		for (ProximityConfig proximityConfig : this.proximityWorlds) {
			if (proximityConfig.enabled()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public ProximityConfig proximity(World world) {
		return this.getWorldEntry(world).proximityConfig;
	}

	@Override
	public byte[] hash() {
		return hash;
	}

	private class WorldEntry {

		private final OrebfuscatorWorldConfig worldConfig;
		private final OrebfuscatorProximityConfig proximityConfig;
		private final OrebfuscatorBlockMask blockMask;

		public WorldEntry(World world) {
			OrebfuscatorWorldConfig worldConfig = null;
			OrebfuscatorProximityConfig proximityConfig = null;

			for (OrebfuscatorWorldConfig config : OrebfuscatorConfig.this.world) {
				for (String worldName : config.worlds()) {
					if (worldName.equalsIgnoreCase(world.getName())) {
						worldConfig = config;
						break;
					}
				}
			}

			for (OrebfuscatorProximityConfig config : OrebfuscatorConfig.this.proximityWorlds) {
				for (String worldName : config.worlds()) {
					if (worldName.equalsIgnoreCase(world.getName())) {
						proximityConfig = config;
						break;
					}
				}
			}

			this.worldConfig = worldConfig;
			this.proximityConfig = proximityConfig;
			this.blockMask = OrebfuscatorBlockMask.create(worldConfig, proximityConfig);
		}
	}
}
