package net.imprex.orebfuscator.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.util.OFCLogger;

public class OrebfuscatorCacheConfig implements CacheConfig {

	private boolean enabled = true;
	private Path baseDirectory = Bukkit.getWorldContainer().toPath().resolve("orebfuscator_cache/");

	private int maximumOpenRegionFiles = 256;
	private long deleteRegionFilesAfterAccess = TimeUnit.DAYS.toMillis(2);
	private boolean enableDiskCache = true;

	private int maximumSize = 8192;
	private long expireAfterAccess = TimeUnit.SECONDS.toMillis(30);

	private int maximumTaskQueueSize = 32768;

	public void deserialize(ConfigurationSection section) {
		this.enabled = section.getBoolean("enabled", true);
		this.deserializeBaseDirectory(section, "orebfuscator_cache/");

		this.maximumOpenRegionFiles = section.getInt("maximumOpenRegionFiles", 256);
		this.deleteRegionFilesAfterAccess = section.getLong("deleteRegionFilesAfterAccess", TimeUnit.DAYS.toMillis(2));
		this.enableDiskCache = section.getBoolean("enableDiskCache", true);

		this.maximumSize = section.getInt("maximumSize", 8192);
		this.expireAfterAccess = section.getLong("expireAfterAccess", TimeUnit.SECONDS.toMillis(30));

		this.maximumTaskQueueSize = section.getInt("maximumTaskQueueSize", 32768);
		this.validateConfigValues();
	}

	public void serialize(ConfigurationSection section) {
		section.set("enabled", this.enabled);
		section.set("baseDirectory", this.baseDirectory.toString());

		section.set("maximumOpenRegionFiles", this.maximumOpenRegionFiles);
		section.set("deleteRegionFilesAfterAccess", this.deleteRegionFilesAfterAccess);
		section.set("enableDiskCache", this.enableDiskCache);

		section.set("maximumSize", this.maximumSize);
		section.set("expireAfterAccess", this.expireAfterAccess);

		section.set("maximumTaskQueueSize", this.maximumTaskQueueSize);
	}

	private void deserializeBaseDirectory(ConfigurationSection section, String defaultPath) {
		Path worldPath = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
		String baseDirectory = section.getString("baseDirectory", defaultPath);

		try {
			this.baseDirectory = Paths.get(baseDirectory).normalize();
		} catch (InvalidPathException e) {
			OFCLogger.warn("config path '" + section.getCurrentPath() + ".baseDirectory' contains malformed path '"
					+ baseDirectory + "', using default path '" + defaultPath + "'");
			this.baseDirectory = worldPath.resolve(defaultPath).normalize();
		}

		OFCLogger.debug("Using '" + this.baseDirectory + "' as chunk cache path");

		if (this.enabled()) {
			try {
				if (Files.notExists(this.baseDirectory)) {
					Files.createDirectories(this.baseDirectory);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void validateConfigValues() {
		if (this.maximumOpenRegionFiles < 1) {
			throw new IllegalArgumentException("cache.maximumOpenRegionFiles is lower than one");
		}
		if (this.deleteRegionFilesAfterAccess < 1) {
			throw new IllegalArgumentException("cache.deleteRegionFilesAfterAccess is lower than one");
		}
		if (this.maximumSize < 1) {
			throw new IllegalArgumentException("cache.maximumSize is lower than one");
		}
		if (this.expireAfterAccess < 1) {
			throw new IllegalArgumentException("cache.expireAfterAccess is lower than one");
		}
		if (this.maximumTaskQueueSize < 1) {
			throw new IllegalArgumentException("cache.maximumTaskQueueSize is lower than one");
		}
	}

	@Override
	public boolean enabled() {
		return this.enabled;
	}

	@Override
	public Path baseDirectory() {
		return this.baseDirectory;
	}

	@Override
	public Path regionFile(ChunkPosition key) {
		return this.baseDirectory.resolve(key.getWorld().getName())
				.resolve("r." + (key.getX() >> 5) + "." + (key.getZ() >> 5) + ".mca");
	}

	@Override
	public int maximumOpenRegionFiles() {
		return this.maximumOpenRegionFiles;
	}

	@Override
	public long deleteRegionFilesAfterAccess() {
		return this.deleteRegionFilesAfterAccess;
	}

	@Override
	public boolean enableDiskCache() {
		return this.enableDiskCache;
	}

	@Override
	public int maximumSize() {
		return this.maximumSize;
	}

	@Override
	public long expireAfterAccess() {
		return this.expireAfterAccess;
	}

	@Override
	public int maximumTaskQueueSize() {
		return this.maximumTaskQueueSize;
	}
}
