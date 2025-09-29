package dev.imprex.orebfuscator.config.api;

public interface BlockFlags {

  public static final int FLAG_OBFUSCATE = 1;
  public static final int FLAG_BLOCK_ENTITY = 2;
  public static final int FLAG_PROXIMITY = 4;
  public static final int FLAG_USE_BLOCK_BELOW = 8;
  public static final int FLAG_ALLOW_FOR_USE_BLOCK_BELOW = 16;

  public static boolean isEmpty(int mask) {
    return (mask & 0xFF) == 0;
  }

  public static boolean isBitSet(int mask, int flag) {
    return (mask & flag) != 0;
  }

  public static boolean isObfuscateBitSet(int mask) {
    return isBitSet(mask, FLAG_OBFUSCATE);
  }

  public static boolean isBlockEntityBitSet(int mask) {
    return isBitSet(mask, FLAG_BLOCK_ENTITY);
  }

  public static boolean isProximityBitSet(int mask) {
    return isBitSet(mask, FLAG_PROXIMITY);
  }

  public static boolean isUseBlockBelowBitSet(int mask) {
    return isBitSet(mask, FLAG_USE_BLOCK_BELOW);
  }

  public static boolean isAllowForUseBlockBelowBitSet(int mask) {
    return isBitSet(mask, FLAG_ALLOW_FOR_USE_BLOCK_BELOW);
  }

  int flags(int blockState);

  int flags(int blockState, int y);
}
