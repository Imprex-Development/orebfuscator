package net.imprex.orebfuscator.nms;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.config.Config;

public abstract class AbstractNmsManager implements NmsManager {

	private static final byte FLAG_AIR = 1;
	private static final byte FLAG_OCCLUDING = 2;
	private static final byte FLAG_TILE_ENTITY = 4;

	private final AbstractRegionFileCache<?> regionFileCache;
	private final Map<Material, Set<Integer>> materialToIds = new HashMap<>();
	private final byte[] blockFlags = new byte[getTotalBlockCount()];

	public AbstractNmsManager(Config config) {
		this.regionFileCache = this.createRegionFileCache(config.cache());
	}

	protected abstract AbstractRegionFileCache<?> createRegionFileCache(CacheConfig cacheConfig);

	protected final void registerMaterialId(Material material, int id) {
		this.materialToIds.computeIfAbsent(material, key -> new HashSet<>()).add(id);
	}

	protected final void setBlockFlags(int blockId, boolean isAir, boolean canOcclude, boolean isTileEntity) {
		byte flags = this.blockFlags[blockId];
		flags |= isAir ? FLAG_AIR : 0;
		flags |= canOcclude ? FLAG_OCCLUDING : 0;
		flags |= isTileEntity ? FLAG_TILE_ENTITY : 0;
		this.blockFlags[blockId] = flags;
	}

	@Override
	public final AbstractRegionFileCache<?> getRegionFileCache() {
		return this.regionFileCache;
	}

	@Override
	public final Set<Integer> getBlockIds(Material material) {
		return Collections.unmodifiableSet(this.materialToIds.get(material));
	}

	@Override
	public final boolean isAir(int blockId) {
		return (this.blockFlags[blockId] & FLAG_AIR) != 0;
	}

	@Override
	public final boolean isOccluding(int blockId) {
		return (this.blockFlags[blockId] & FLAG_OCCLUDING) != 0;
	}

	@Override
	public final boolean isBlockEntity(int blockId) {
		return (this.blockFlags[blockId] & FLAG_TILE_ENTITY) != 0;
	}

	@Override
	public final void close() {
		this.regionFileCache.clear();
	}
}
