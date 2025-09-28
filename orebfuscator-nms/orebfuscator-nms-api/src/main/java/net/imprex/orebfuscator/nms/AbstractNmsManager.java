package net.imprex.orebfuscator.nms;

import java.util.HashMap;
import java.util.Map;

import net.imprex.orebfuscator.util.BlockProperties;
import net.imprex.orebfuscator.util.BlockStateProperties;
import net.imprex.orebfuscator.util.MathUtil;
import net.imprex.orebfuscator.util.NamespacedKey;

public abstract class AbstractNmsManager implements NmsManager {

  private final AbstractRegionFileCache<?> regionFileCache;

  private final int uniqueBlockStateCount;
  private final int maxBitsPerBlockState;

  private final BlockStateProperties[] blockStates;
  private final Map<NamespacedKey, BlockProperties> blocks = new HashMap<>();

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
  public final BlockProperties getBlockByName(NamespacedKey key) {
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
