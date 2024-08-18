package net.imprex.orebfuscator.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.config.context.ConfigParsingContext;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.util.OFCLogger;

public class OrebfuscatorCacheConfig implements CacheConfig {

	private boolean enabled = true;

	private int maximumSize = 8192;
	private long expireAfterAccess = TimeUnit.SECONDS.toMillis(30);

	private boolean enableDiskCache = true;
	private Path baseDirectory = Bukkit.getWorldContainer().toPath().resolve("orebfuscator_cache/");
	private int maximumOpenRegionFiles = 256;
	private long deleteRegionFilesAfterAccess = TimeUnit.DAYS.toMillis(2);
	private int maximumTaskQueueSize = 32768;

	public void deserialize(ConfigurationSection section, ConfigParsingContext context) {
		this.enabled = section.getBoolean("enabled", true);

		// parse memoryCache section
		ConfigParsingContext memoryContext = context.section("memoryCache");
		ConfigurationSection memorySection = section.getConfigurationSection("memoryCache");
		if (memorySection != null) {
			this.maximumSize = memorySection.getInt("maximumSize", 8192);
			memoryContext.errorMinValue("maximumSize", 1, this.maximumSize);

			this.expireAfterAccess = memorySection.getLong("expireAfterAccess", TimeUnit.SECONDS.toMillis(30));
			memoryContext.errorMinValue("expireAfterAccess", 1, this.expireAfterAccess);
		} else {
			memoryContext.warnMissingSection();
		}

		// parse diskCache section, isolate errors to disable only diskCache on section error
		ConfigParsingContext diskContext = context.section("diskCache", true);
		ConfigurationSection diskSection = section.getConfigurationSection("diskContext");
		if (diskSection != null) {
			this.enableDiskCache = diskSection.getBoolean("enabled", true);
			this.deserializeBaseDirectory(diskSection, diskContext, "orebfuscator_cache/");

			this.maximumOpenRegionFiles = diskSection.getInt("maximumOpenFiles", 256);
			diskContext.errorMinValue("maximumOpenFiles", 1, this.maximumOpenRegionFiles);

			this.deleteRegionFilesAfterAccess = diskSection.getLong("deleteFilesAfterAccess", TimeUnit.DAYS.toMillis(2));
			diskContext.errorMinValue("deleteFilesAfterAccess", 1, this.deleteRegionFilesAfterAccess);

			this.maximumTaskQueueSize = diskSection.getInt("maximumTaskQueueSize", 32768);
			diskContext.errorMinValue("maximumTaskQueueSize", 1, this.maximumTaskQueueSize);
		} else {
			diskContext.warnMissingSection();
		}

		// try create diskCache.directory
		OFCLogger.debug("Using '" + this.baseDirectory + "' as chunk cache path");
		try {
			if (Files.notExists(this.baseDirectory)) {
				Files.createDirectories(this.baseDirectory);
			}
		} catch (IOException e) {
			context.error(String.format("can't create cache directory {error(%s)}", e.getMessage()));
			e.printStackTrace();
		}

		// disable parts if the config contain errors
		this.enabled &= !context.hasErrors();
		this.enableDiskCache &= !diskContext.hasErrors();

	}

	public void serialize(ConfigurationSection section) {
		section.set("enabled", this.enabled);

		section.set("memoryCache.maximumSize", this.maximumSize);
		section.set("memoryCache.expireAfterAccess", this.expireAfterAccess);

		section.set("diskCache.enabled", this.enableDiskCache);
		section.set("diskCache.directory", this.baseDirectory.toString());
		section.set("diskCache.maximumOpenFiles", this.maximumOpenRegionFiles);
		section.set("diskCache.deleteFilesAfterAccess", this.deleteRegionFilesAfterAccess);
		section.set("diskCache.maximumTaskQueueSize", this.maximumTaskQueueSize);
	}

	private void deserializeBaseDirectory(ConfigurationSection section, ConfigParsingContext context, String defaultPath) {
		Path worldPath = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
		String baseDirectory = section.getString("directory", defaultPath);

		try {
			this.baseDirectory = Paths.get(baseDirectory).normalize();
		} catch (InvalidPathException e) {
			context.warn("directory", String.format("contains malformed path {value(%s)}; using default path '%s'",
					baseDirectory, defaultPath));
			this.baseDirectory = worldPath.resolve(defaultPath).normalize();
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
		return this.baseDirectory.resolve(key.world)
				.resolve("r." + (key.x >> 5) + "." + (key.z >> 5) + ".mca");
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
