package net.imprex.orebfuscator.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import com.lishid.orebfuscator.utils.Globals;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.util.OFCLogger;

public class OrebfuscatorConfig implements Config {

	private static final int CONFIG_VERSION = 1;

	private final OrebfuscatorGeneralConfig generalConfig = new OrebfuscatorGeneralConfig();
	private final OrebfuscatorCacheConfig cacheConfig = new OrebfuscatorCacheConfig();

	private final List<OrebfuscatorWorldConfig> world = new ArrayList<>();
	private final List<OrebfuscatorProximityConfig> proximityWorlds = new ArrayList<>();

	private final Map<World, OrebfuscatorBlockMask> worldToBlockMask = new HashMap<>();
	private final Map<World, OrebfuscatorWorldConfig> worldToWorldConfig = new HashMap<>();
	private final Map<World, OrebfuscatorProximityConfig> worldToProximityConfig = new HashMap<>();

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

		NmsInstance.close();
		NmsInstance.initialize(this);

		this.initialize();
	}

	private void createConfigIfNotExist() {
		Path dataFolder = this.plugin.getDataFolder().toPath();
		Path path = dataFolder.resolve("config.yml");

		if (Files.notExists(path)) {
			try {
				Matcher matcher = Globals.NMS_PATTERN.matcher(Globals.SERVER_VERSION);

				if (!matcher.find()) {
					throw new RuntimeException("Can't parse server version " + Globals.SERVER_VERSION);
				}

				String configVersion = matcher.group(1) + "." + matcher.group(2);

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

	private void initialize() {
		this.worldToWorldConfig.clear();

		for (OrebfuscatorWorldConfig worldConfig : this.world) {
			worldConfig.initialize();
			for (World world : worldConfig.worlds()) {
				if (this.worldToWorldConfig.containsKey(world)) {
					OFCLogger.warn("world " + world.getName() + " has more than one world config choosing first one");
				} else {
					this.worldToWorldConfig.put(world, worldConfig);
				}
			}
		}

		for (World world : Bukkit.getWorlds()) {
			if (!this.worldToWorldConfig.containsKey(world)) {
				OFCLogger.warn("world " + world.getName() + " is missing a world config");
			}
		}

		for (OrebfuscatorProximityConfig proximityConfig : this.proximityWorlds) {
			proximityConfig.initialize();
			for (World world : proximityConfig.worlds()) {
				if (this.worldToProximityConfig.containsKey(world)) {
					OFCLogger.warn("world " + world.getName() + " has more than one proximity config choosing first one");
				} else {
					this.worldToProximityConfig.put(world, proximityConfig);
				}
			}
		}

		for (World world : Bukkit.getWorlds()) {
			if (!this.worldToWorldConfig.containsKey(world)) {
				OFCLogger.warn("world " + world.getName() + " is missing a world config");
			}

			OrebfuscatorWorldConfig worldConfig = this.worldToWorldConfig.get(world);
			OrebfuscatorProximityConfig proximityConfig = this.worldToProximityConfig.get(world);
			this.worldToBlockMask.put(world, new OrebfuscatorBlockMask(worldConfig, proximityConfig));
		}
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
		return this.worldToBlockMask.get(Objects.requireNonNull(world));
	}

	@Override
	public boolean needsObfuscation(World world) {
		WorldConfig worldConfig = this.world(world);
		ProximityConfig proximityConfig = this.proximity(world);
		return worldConfig != null && worldConfig.enabled() || proximityConfig != null && proximityConfig.enabled();
	}

	@Override
	public OrebfuscatorWorldConfig world(World world) {
		return this.worldToWorldConfig.get(Objects.requireNonNull(world));
	}

	@Override
	public boolean proximityEnabled() {
		// TODO check if at least one config is enabled
		return this.proximityWorlds.size() != 0;
	}

	@Override
	public ProximityConfig proximity(World world) {
		return this.worldToProximityConfig.get(Objects.requireNonNull(world));
	}

	@Override
	public byte[] hash() {
		return hash;
	}
}
