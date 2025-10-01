package dev.imprex.orebfuscator.config;

import java.util.Map.Entry;
import dev.imprex.orebfuscator.config.api.BlockFlags;
import dev.imprex.orebfuscator.config.components.ConfigBlockValue;
import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.util.BlockProperties;
import dev.imprex.orebfuscator.util.BlockStateProperties;

public class OrebfuscatorBlockFlags implements BlockFlags {

  private static final BlockFlags EMPTY_FLAGS = new EmptyBlockFlags();

  static BlockFlags create(RegistryAccessor registry, OrebfuscatorObfuscationConfig worldConfig,
      OrebfuscatorProximityConfig proximityConfig) {
    if ((worldConfig != null && worldConfig.isEnabled()) || (proximityConfig != null
        && proximityConfig.isEnabled())) {
      return new OrebfuscatorBlockFlags(registry, worldConfig, proximityConfig);
    }
    return EMPTY_FLAGS;
  }

  private final int[] blockFlags;

  private OrebfuscatorBlockFlags(RegistryAccessor registry, OrebfuscatorObfuscationConfig worldConfig,
      OrebfuscatorProximityConfig proximityConfig) {
    this.blockFlags = new int[registry.getUniqueBlockStateCount()];

    if (worldConfig != null && worldConfig.isEnabled()) {
      for (ConfigBlockValue blockValue : worldConfig.hiddenBlocks()) {
        for (BlockProperties block : blockValue.blocks()) {
          this.setBlockBits(block, FLAG_OBFUSCATE);
        }
      }
    }

    if (proximityConfig != null && proximityConfig.isEnabled()) {
      for (Entry<ConfigBlockValue, Integer> entry : proximityConfig.hiddenBlocks()) {
        for (BlockProperties block : entry.getKey().blocks()) {
          this.setBlockBits(block, entry.getValue());
        }
      }
      for (BlockProperties block : proximityConfig.allowForUseBlockBelow()) {
        this.setBlockBits(block, FLAG_ALLOW_FOR_USE_BLOCK_BELOW);
      }
    }
  }

  private void setBlockBits(BlockProperties block, int bits) {
    for (BlockStateProperties blockState : block.getBlockStates()) {
      int blockMask = this.blockFlags[blockState.getId()] | bits;

      if (blockState.isBlockEntity()) {
        blockMask |= FLAG_BLOCK_ENTITY;
      }

      this.blockFlags[blockState.getId()] = blockMask;
    }
  }

  @Override
  public int flags(int blockState) {
    return this.blockFlags[blockState];
  }

  @Override
  public int flags(int blockState, int y) {
    int flags = this.blockFlags[blockState];

    if (ProximityHeightCondition.match(flags, y)) {
      flags |= FLAG_PROXIMITY;
    }

    return flags;
  }

  private static class EmptyBlockFlags implements BlockFlags {

    @Override
    public int flags(int blockState) {
      return 0;
    }

    @Override
    public int flags(int blockState, int y) {
      return 0;
    }
  }
}
