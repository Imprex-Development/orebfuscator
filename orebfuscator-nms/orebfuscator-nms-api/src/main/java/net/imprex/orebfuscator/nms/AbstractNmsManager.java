package net.imprex.orebfuscator.nms;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.imprex.orebfuscator.cache.AbstractRegionFileCache;
import dev.imprex.orebfuscator.util.BlockProperties;
import dev.imprex.orebfuscator.util.BlockStateProperties;
import dev.imprex.orebfuscator.util.BlockTag;
import dev.imprex.orebfuscator.util.MathUtil;
import dev.imprex.orebfuscator.util.NamespacedKey;

public abstract class AbstractNmsManager implements NmsManager {

	private final AbstractRegionFileCache<?> regionFileCache;

	private final int uniqueBlockStateCount;
	private final int maxBitsPerBlockState;

	private final BlockStateProperties[] blockStates;
	private final Map<NamespacedKey, BlockProperties> blocks = new HashMap<>();
	protected final Map<NamespacedKey, BlockTag> tags = new HashMap<>();

	public AbstractNmsManager(int uniqueBlockStateCount, AbstractRegionFileCache<?> regionFileCache) {
		this.regionFileCache = regionFileCache;

		this.uniqueBlockStateCount = uniqueBlockStateCount;
		this.maxBitsPerBlockState = MathUtil.ceilLog2(uniqueBlockStateCount);

		this.blockStates = new BlockStateProperties[uniqueBlockStateCount];
	}

	protected final void registerBlockProperties(BlockProperties block) {
		this.blocks.put(block.getKey(), block);

		for (BlockStateProperties blockState : block.getBlockStates()) {
			this.blockStates[blockState.getId()] = blockState;
		}
	}

	protected final void registerBlockTag(BlockTag tag) {
		this.tags.put(tag.key(), tag);
	}

	@Override
	public final AbstractRegionFileCache<?> getRegionFileCache() {
		return this.regionFileCache;
	}

	@Override
	public final int getUniqueBlockStateCount() {
		return this.uniqueBlockStateCount;
	}

	@Override
	public final int getMaxBitsPerBlockState() {
		return this.maxBitsPerBlockState;
	}

	@Override
	public final @Nullable BlockProperties getBlockByName(@NotNull String name) {
		return this.blocks.get(NamespacedKey.fromString(name));
	}

	@Override
	public final @Nullable BlockTag getBlockTagByName(@NotNull String name) {
		return this.tags.get(NamespacedKey.fromString(name));
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
