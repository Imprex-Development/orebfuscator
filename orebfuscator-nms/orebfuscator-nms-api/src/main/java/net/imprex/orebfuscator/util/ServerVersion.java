package net.imprex.orebfuscator.util;

public class ServerVersion {

  private static final boolean IS_MOJANG_MAPPED = classExists("net.minecraft.core.BlockPos")
      && fieldExists("net.minecraft.world.level.block.Blocks", "AIR");
  private static final boolean IS_FOLIA = classExists("io.papermc.paper.threadedregions.RegionizedServer");
  private static final boolean IS_PAPER = !IS_FOLIA && classExists("com.destroystokyo.paper.PaperConfig");
  private static final boolean IS_BUKKIT = !IS_FOLIA && !IS_PAPER;

  private static boolean classExists(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  private static boolean fieldExists(String className, String fieldName) {
    try {
      Class<?> target = Class.forName(className);
      return target.getDeclaredField(fieldName) != null;
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isMojangMapped() {
    return IS_MOJANG_MAPPED;
  }

  public static boolean isFolia() {
    return IS_FOLIA;
  }

  public static boolean isPaper() {
    return IS_PAPER;
  }

  public static boolean isBukkit() {
    return IS_BUKKIT;
  }
}
