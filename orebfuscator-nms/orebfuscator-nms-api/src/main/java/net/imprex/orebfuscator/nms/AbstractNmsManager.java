package net.imprex.orebfuscator.nms;

import java.util.HashMap;
import java.util.Map;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.util.BlockProperties;
import net.imprex.orebfuscator.util.BlockStateProperties;

public abstract class AbstractNmsManager implements NmsManager {

	private final AbstractRegionFileCache<?> regionFileCache;

	private final BlockStateProperties[] blockStates = new BlockStateProperties[getTotalBlockCount()];
	private final Map<String, BlockProperties> blocks = new HashMap<>();

	public AbstractNmsManager(Config config) {
		this.regionFileCache = this.createRegionFileCache(config.cache());
	}

	protected abstract AbstractRegionFileCache<?> createRegionFileCache(CacheConfig cacheConfig);

	protected final void registerBlockStateProperties(BlockStateProperties properties) {
		this.blockStates[properties.getId()] = properties;
	}

	protected final void registerBlockProperties(BlockProperties properties) {
		this.blocks.put(properties.getName(), properties);
	}

	protected final BlockStateProperties getBlockStateProperties(int id) {
		return this.blockStates[id];
	}

	@Override
	public final AbstractRegionFileCache<?> getRegionFileCache() {
		return this.regionFileCache;
	}

	@Override
	public final BlockProperties getBlockByName(String key) {
		return this.blocks.get(key);
	}

	@Override
	public final boolean isAir(int id) {
		return this.blockStates[id].isAir();
	}

	@Override
	public final boolean isOccluding(int id) {
		return this.blockStates[id].isOccluding();
	}

	@Override
	public final boolean isBlockEntity(int id) {
		return this.blockStates[id].isBlockEntity();
	}

	@Override
	public final void close() {
		this.regionFileCache.clear();
	}
}
