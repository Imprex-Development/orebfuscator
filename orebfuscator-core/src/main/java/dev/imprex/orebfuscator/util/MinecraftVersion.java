package dev.imprex.orebfuscator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;

import dev.imprex.orebfuscator.logging.OfcLogger;

public final class MinecraftVersion {

  private static final class NmsMapping {

    private static final List<NmsMapping> MAPPINGS = new ArrayList<>();

    static {
      MAPPINGS.add(new NmsMapping("1.21.6", "v1_21_R5"));
      MAPPINGS.add(new NmsMapping("1.21.5", "v1_21_R4"));
      MAPPINGS.add(new NmsMapping("1.21.4", "v1_21_R3"));
      MAPPINGS.add(new NmsMapping("1.21.2", "v1_21_R2"));
      MAPPINGS.add(new NmsMapping("1.21", "v1_21_R1"));
      MAPPINGS.add(new NmsMapping("1.20.5", "v1_20_R4"));
    }

    public static String get(Version version) {
      for (NmsMapping mapping : MAPPINGS) {
        if (version.isAtOrAbove(mapping.version)) {
          if (mapping.version.minor() != version.minor()) {
            OfcLogger.warn(String.format("Using nms mapping with mismatched minor versions: %s - %s",
                mapping.version, version));
          }

          return mapping.nmsVersion;
        }
      }

      throw new RuntimeException("Can't get nms package version for minecraft version: " + version);
    }

    private final Version version;
    private final String nmsVersion;

    public NmsMapping(String version, String nmsVersion) {
      this.version = Version.parse(version);
      this.nmsVersion = nmsVersion;
    }
  }

  private static final Pattern PACKAGE_PATTERN = Pattern.compile("org\\.bukkit\\.craftbukkit\\.(v\\d+_\\d+_R\\d+)");
  private static final Version CURRENT_VERSION = Version.parse(Bukkit.getBukkitVersion());

  private static String NMS_VERSION;

  static {
    String craftBukkitPackage = Bukkit.getServer().getClass().getPackage().getName();
    Matcher matcher = PACKAGE_PATTERN.matcher(craftBukkitPackage);

    if (matcher.find()) {
      NMS_VERSION = matcher.group(1);
    } else {
      NMS_VERSION = NmsMapping.get(CURRENT_VERSION);
    }
  }

  public static String nmsVersion() {
    return NMS_VERSION;
  }

  public static Version current() {
    return CURRENT_VERSION;
  }

  public static int majorVersion() {
    return CURRENT_VERSION.major();
  }

  public static int minorVersion() {
    return CURRENT_VERSION.minor();
  }

  public static int patchVersion() {
    return CURRENT_VERSION.patch();
  }

  public static boolean isAbove(String version) {
    return CURRENT_VERSION.isAbove(Version.parse(version));
  }

  public static boolean isAtOrAbove(String version) {
    return CURRENT_VERSION.isAtOrAbove(Version.parse(version));
  }

  public static boolean isAtOrBelow(String version) {
    return CURRENT_VERSION.isAtOrBelow(Version.parse(version));
  }

  public static boolean isBelow(String version) {
    return CURRENT_VERSION.isBelow(Version.parse(version));
  }
}
