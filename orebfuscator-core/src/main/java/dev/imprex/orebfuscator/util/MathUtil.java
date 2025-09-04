package dev.imprex.orebfuscator.util;

public class MathUtil {

  public static int ceilToPowerOfTwo(int value) {
    value--;
    value |= value >> 1;
    value |= value >> 2;
    value |= value >> 4;
    value |= value >> 8;
    value |= value >> 16;
    value++;
    return value;
  }

  public static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(max, value));
  }

  public static int ceilLog2(int value) {
    int result = 31 - Integer.numberOfLeadingZeros(value);
    // add 1 if value is NOT a power of 2 (to do the ceil)
    return result + (value != 0 && (value & value - 1) == 0 ? 0 : 1);
  }
}
