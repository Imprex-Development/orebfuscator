package net.imprex.orebfuscator.nms;

import com.lishid.orebfuscator.nms.INmsManager;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.config.Config;

public abstract class AbstractNmsManager implements INmsManager {

	private final CacheConfig cacheConfig;
	private final AbstractRegionFileCache<?> regionFileCache;

	public AbstractNmsManager(Config config) {
		this.cacheConfig = config.cache();

		this.regionFileCache = this.createRegionFileCache(this.cacheConfig);
	}

	protected abstract AbstractRegionFileCache<?> createRegionFileCache(CacheConfig cacheConfig);

	@Override
	public final AbstractRegionFileCache<?> getRegionFileCache() {
		return this.regionFileCache;
	}
}
