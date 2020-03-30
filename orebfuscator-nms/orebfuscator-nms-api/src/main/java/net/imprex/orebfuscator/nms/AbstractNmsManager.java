package net.imprex.orebfuscator.nms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.config.Config;

public abstract class AbstractNmsManager implements NmsManager {

	private final AbstractRegionFileCache<?> regionFileCache;
	private final Map<Material, Set<Integer>> materialToIds = new HashMap<>();

	public AbstractNmsManager(Config config) {
		this.regionFileCache = this.createRegionFileCache(config.cache());
	}

	protected abstract AbstractRegionFileCache<?> createRegionFileCache(CacheConfig cacheConfig);

	protected final void registerMaterialId(Material material, int id) {
		this.materialToIds.computeIfAbsent(material, key -> new HashSet<>()).add(id);
	}

	protected final Set<Integer> mergeMaterialIds(Set<Material> materials) {
		return mergeMaterialIds(materials.toArray(new Material[materials.size()]));
	}

	protected final Set<Integer> mergeMaterialIds(Material...materials) {
		Set<Integer> materialIds = new HashSet<>();
		for (Material material : materials) {
			materialIds.addAll(this.getMaterialIds(material));
		}
		return materialIds;
	}

	@Override
	public final AbstractRegionFileCache<?> getRegionFileCache() {
		return this.regionFileCache;
	}

	@Override
	public final Set<Integer> getMaterialIds(Material material) {
		return this.materialToIds.get(material);
	}

	@Override
	public final void close() {
		this.regionFileCache.clear();
	}
}
