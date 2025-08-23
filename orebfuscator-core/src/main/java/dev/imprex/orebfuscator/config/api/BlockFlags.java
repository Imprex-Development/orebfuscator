package dev.imprex.orebfuscator.config.api;

public interface BlockFlags {

  int FLAG_OBFUSCATE = 1;
  int FLAG_BLOCK_ENTITY = 2;
  int FLAG_PROXIMITY = 4;
  int FLAG_USE_BLOCK_BELOW = 8;
  int FLAG_ALLOW_FOR_USE_BLOCK_BELOW = 16;

  static boolean isEmpty(int mask) {
    return (mask & 0xFF) == 0;
  }

  static boolean isBitSet(int mask, int flag) {
    return (mask & flag) != 0;
  }

  static boolean isObfuscateBitSet(int mask) {
    return isBitSet(mask, FLAG_OBFUSCATE);
  }

  static boolean isBlockEntityBitSet(int mask) {
    return isBitSet(mask, FLAG_BLOCK_ENTITY);
  }

  static boolean isProximityBitSet(int mask) {
    return isBitSet(mask, FLAG_PROXIMITY);
  }

  static boolean isUseBlockBelowBitSet(int mask) {
    return isBitSet(mask, FLAG_USE_BLOCK_BELOW);
  }

  static boolean isAllowForUseBlockBelowBitSet(int mask) {
    return isBitSet(mask, FLAG_ALLOW_FOR_USE_BLOCK_BELOW);
  }

  int flags(int blockState);

  int flags(int blockState, int y);
}
