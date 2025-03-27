package net.imprex.orebfuscator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;

public final class MinecraftVersion implements Comparable<MinecraftVersion> {

	private static final class NmsMapping {

		private static final List<NmsMapping> MAPPINGS = new ArrayList<>();

		static {
			MAPPINGS.add(new NmsMapping("1.21.5", "v1_21_R4"));
			MAPPINGS.add(new NmsMapping("1.21.4", "v1_21_R3"));
			MAPPINGS.add(new NmsMapping("1.21.2", "v1_21_R2"));
			MAPPINGS.add(new NmsMapping("1.21", "v1_21_R1"));
			MAPPINGS.add(new NmsMapping("1.20.5", "v1_20_R4"));
		}

		public static String get(MinecraftVersion version) {
			for (NmsMapping mapping : MAPPINGS) {
				if (version.isAtOrAbove(mapping.version)) {
					if (mapping.version.minor() != version.minor()) {
						OFCLogger.warn(String.format("Using nms mapping with mismatched minor versions: %s - %s",
								mapping.version, version));
					}

					return mapping.nmsVersion;
				}
			}

			throw new RuntimeException("Can't get nms package version for minecraft version: " + version);
		}

		private final MinecraftVersion version;
		private final String nmsVersion;

		public NmsMapping(String version, String nmsVersion) {
			this.version = new MinecraftVersion(version);
			this.nmsVersion = nmsVersion;
		}
	}

	private static final Pattern VERSION_PATTERN = Pattern.compile("(?<major>\\d+)(?:\\.(?<minor>\\d+))(?:\\.(?<patch>\\d+))?");
	private static final Pattern PACKAGE_PATTERN = Pattern.compile("org\\.bukkit\\.craftbukkit\\.(v\\d+_\\d+_R\\d+)");

	private static final MinecraftVersion CURRENT_VERSION = new MinecraftVersion(Bukkit.getBukkitVersion());

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

	public static int majorVersion() {
		return CURRENT_VERSION.major;
	}

	public static int minorVersion() {
		return CURRENT_VERSION.minor;
	}

	public static int patchVersion() {
		return CURRENT_VERSION.patch;
	}

	public static boolean isAbove(String version) {
		return CURRENT_VERSION.isAbove(new MinecraftVersion(version));
	}

	public static boolean isAtOrAbove(String version) {
		return CURRENT_VERSION.isAtOrAbove(new MinecraftVersion(version));
	}

	public static boolean isAtOrBelow(String version) {
		return CURRENT_VERSION.isAtOrBelow(new MinecraftVersion(version));
	}

	public static boolean isBelow(String version) {
		return CURRENT_VERSION.isBelow(new MinecraftVersion(version));
	}

	private final int major;
	private final int minor;
	private final int patch;

	public MinecraftVersion(String version) {
		Matcher matcher = VERSION_PATTERN.matcher(version);

		if (!matcher.find()) {
			throw new IllegalArgumentException("can't parse minecraft version: " + version);
		}

		this.major = Integer.parseInt(matcher.group("major"));
		this.minor = Integer.parseInt(matcher.group("minor"));

		String patch = matcher.group("patch");
		if (patch != null) {
			this.patch = Integer.parseInt(patch);
		} else {
			this.patch = 0;
		}
	}

	public int major() {
		return this.major;
	}

	public int minor() {
		return this.minor;
	}

	public int patch() {
		return this.patch;
	}

	public boolean isAbove(MinecraftVersion version) {
		return this.compareTo(version) > 0;
	}

	public boolean isAtOrAbove(MinecraftVersion version) {
		return this.compareTo(version) >= 0;
	}

	public boolean isAtOrBelow(MinecraftVersion version) {
		return this.compareTo(version) <= 0;
	}

	public boolean isBelow(MinecraftVersion version) {
		return this.compareTo(version) < 0;
	}

	@Override
	public int compareTo(MinecraftVersion other) {
		int major = Integer.compare(this.major, other.major);
		if (major != 0) {
			return major;
		}

		int minor = Integer.compare(this.minor, other.minor);
		if (minor != 0) {
			return minor;
		}

		return Integer.compare(this.patch, other.patch);
	}

	@Override
	public int hashCode() {
		return Objects.hash(major, minor, patch);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MinecraftVersion)) {
			return false;
		}
		MinecraftVersion other = (MinecraftVersion) obj;
		return major == other.major && minor == other.minor && patch == other.patch;
	}

	@Override
	public String toString() {
		return String.format("%s.%s.%s", this.major, this.minor, this.patch);
	}
}
