package net.imprex.orebfuscator.nms;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.util.SimpleCache;

public abstract class AbstractRegionFileCache<RegionFile> {

	protected final ReadWriteLock lock = new ReentrantReadWriteLock(true);
	protected final Map<Path, RegionFile> regionFiles;

	protected final CacheConfig cacheConfig;

	public AbstractRegionFileCache(CacheConfig cacheConfig) {
		this.cacheConfig = cacheConfig;

		this.regionFiles = new SimpleCache<>(cacheConfig.maximumOpenRegionFiles(), this::remove);
	}

	protected abstract RegionFile createRegionFile(Path path) throws IOException;

	protected abstract void closeRegionFile(RegionFile t) throws IOException;

	protected abstract DataInputStream createInputStream(RegionFile t, ChunkPosition key) throws IOException;

	protected abstract DataOutputStream createOutputStream(RegionFile t, ChunkPosition key) throws IOException;

	public final DataInputStream createInputStream(ChunkPosition key) throws IOException {
		RegionFile t = this.get(this.cacheConfig.regionFile(key));
		return t != null ? this.createInputStream(t, key) : null;
	}

	public final DataOutputStream createOutputStream(ChunkPosition key) throws IOException {
		RegionFile t = this.get(this.cacheConfig.regionFile(key));
		return t != null ? this.createOutputStream(t, key) : null;
	}

	private final void remove(Map.Entry<Path, RegionFile> entry) {
		try {
			this.closeRegionFile(entry.getValue());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected final RegionFile get(Path path) throws IOException {
		this.lock.readLock().lock();
		try {
			RegionFile t = this.regionFiles.get(path);
			if (t != null) {
				return t;
			}
		} finally {
			this.lock.readLock().unlock();
		}

		if (Files.notExists(path.getParent())) {
			Files.createDirectories(path.getParent());
		}

		if (this.regionFiles.size() > this.cacheConfig.maximumOpenRegionFiles()) {
			throw new IllegalStateException(String.format("RegionFileCache got bigger than expected (%d > %d)",
					this.regionFiles.size(), this.cacheConfig.maximumOpenRegionFiles()));
		}

		RegionFile t = Objects.requireNonNull(this.createRegionFile(path));

		this.lock.writeLock().lock();
		try {
			this.regionFiles.putIfAbsent(path, t);
			return this.regionFiles.get(path);
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	public final void close(Path path) throws IOException {
		this.lock.writeLock().lock();
		try {
			RegionFile t = this.regionFiles.remove(path);
			if (t != null) {
				this.closeRegionFile(t);
			}
		} finally {
			this.lock.writeLock().unlock();
		}
	}

	public final void clear() {
		this.lock.writeLock().lock();
		try {
			for (RegionFile t : this.regionFiles.values()) {
				try {
					if (t != null) {
						this.closeRegionFile(t);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			this.regionFiles.clear();
		} finally {
			this.lock.writeLock().unlock();
		}
	}
}
