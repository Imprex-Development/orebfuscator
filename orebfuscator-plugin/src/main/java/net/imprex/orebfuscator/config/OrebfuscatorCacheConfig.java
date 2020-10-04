package net.imprex.orebfuscator.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.util.OFCLogger;

public class OrebfuscatorCacheConfig implements CacheConfig {

	private boolean enabled = true;
	private Path baseDirectory = Bukkit.getWorldContainer().toPath().resolve("orebfuscator_cache/");

	private int maximumOpenRegionFiles = 256;
	private long deleteRegionFilesAfterAccess = TimeUnit.DAYS.toMillis(2);

	private int maximumSize = 8192;
	private long expireAfterAccess = TimeUnit.SECONDS.toMillis(30);

	private int maximumTaskQueueSize = 32768;
	private int protocolLibThreads = -1;
	private boolean protocolLibThreadsSet = false;

	public void serialize(ConfigurationSection section) {
		this.enabled(section.getBoolean("enabled", true));
		this.serializeBaseDirectory(section, "orebfuscator_cache/");

		this.maximumOpenRegionFiles(section.getInt("maximumOpenRegionFiles", 256));
		this.deleteRegionFilesAfterAccess(section.getLong("deleteRegionFilesAfterAccess", TimeUnit.DAYS.toMillis(2)));

		this.maximumSize(section.getInt("maximumSize", 8192));
		this.expireAfterAccess(section.getLong("expireAfterAccess", TimeUnit.SECONDS.toMillis(30)));

		this.maximumTaskQueueSize(section.getInt("maximumTaskQueueSize", 32768));
		this.protocolLibThreads(section.getInt("protocolLibThreads", -1));
	}

	public void deserialize(ConfigurationSection section) {
		section.set("enabled", this.enabled);
		section.set("baseDirectory", Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize()
				.relativize(this.baseDirectory).normalize().toString());

		section.set("maximumOpenRegionFiles", this.maximumOpenRegionFiles);
		section.set("deleteRegionFilesAfterAccess", this.deleteRegionFilesAfterAccess);

		section.set("maximumSize", this.maximumSize);
		section.set("expireAfterAccess", this.expireAfterAccess);

		section.set("maximumTaskQueueSize", this.maximumTaskQueueSize);
		section.set("protocolLibThreads", this.protocolLibThreadsSet ? this.protocolLibThreads : -1);
	}

	private void serializeBaseDirectory(ConfigurationSection section, String defaultPath) {
		Path worldPath = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
		String baseDirectory = section.getString("baseDirectory", defaultPath);

		try {
			this.baseDirectory = worldPath.resolve(baseDirectory).normalize();
		} catch (InvalidPathException e) {
			OFCLogger.log(Level.WARNING,
					"config path '" + section.getCurrentPath() + ".baseDirectory' contains malformed path '"
							+ baseDirectory + "', using default path '" + defaultPath + "'");
			this.baseDirectory = worldPath.resolve(defaultPath).normalize();
		}

		if (!this.baseDirectory.startsWith(worldPath)) {
			OFCLogger.log(Level.WARNING,
					"config path '" + section.getCurrentPath() + ".baseDirectory' is no child directory of '"
							+ worldPath + "', using default path: '" + defaultPath + "'");
			this.baseDirectory = worldPath.resolve(defaultPath).normalize();
		}

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

	@Override
	public boolean enabled() {
		return this.enabled;
	}

	@Override
	public void enabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public Path baseDirectory() {
		return this.baseDirectory;
	}

	@Override
	public void baseDirectory(Path path) {
		this.baseDirectory = path;
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
	public void maximumOpenRegionFiles(int count) {
		if (count < 1) {
			throw new IllegalArgumentException("cache.maximumOpenRegionFiles is lower than one");
		}
		this.maximumOpenRegionFiles = count;
	}

	@Override
	public long deleteRegionFilesAfterAccess() {
		return this.deleteRegionFilesAfterAccess;
	}

	@Override
	public void deleteRegionFilesAfterAccess(long expire) {
		if (expire < 1) {
			throw new IllegalArgumentException("cache.deleteRegionFilesAfterAccess is lower than one");
		}
		this.deleteRegionFilesAfterAccess = expire;
	}

	@Override
	public int maximumSize() {
		return this.maximumSize;
	}

	@Override
	public void maximumSize(int size) {
		if (size < 1) {
			throw new IllegalArgumentException("cache.maximumSize is lower than one");
		}
		this.maximumSize = size;
	}

	@Override
	public long expireAfterAccess() {
		return this.expireAfterAccess;
	}

	@Override
	public void expireAfterAccess(long expire) {
		if (expire < 1) {
			throw new IllegalArgumentException("cache.expireAfterAccess is lower than one");
		}
		this.expireAfterAccess = expire;
	}

	@Override
	public int maximumTaskQueueSize() {
		return this.maximumTaskQueueSize;
	}

	@Override
	public void maximumTaskQueueSize(int size) {
		if (size < 1) {
			throw new IllegalArgumentException("cache.maximumTaskQueueSize is lower than one");
		}
		this.maximumTaskQueueSize = size;
	}

	@Override
	public int protocolLibThreads() {
		return this.protocolLibThreads;
	}

	@Override
	public void protocolLibThreads(int threads) {
		if (threads < 1) {
			this.protocolLibThreads = Runtime.getRuntime().availableProcessors();
			OFCLogger.info("cache.protocolLibThreads is less than one, choosing processor count as value = "
					+ this.protocolLibThreads);
			this.protocolLibThreadsSet = false;
		} else {
			this.protocolLibThreads = threads;
			this.protocolLibThreadsSet = true;
		}
	}
}
