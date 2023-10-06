package net.imprex.orebfuscator.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;

public final class MinecraftVersion {

	private static final Pattern VERSION_PATTERN = Pattern.compile("org\\.bukkit\\.craftbukkit\\.(v(\\d+)_(\\d+)_R(\\d+))");

	private static final String NMS_VERSION;
	private static final int MAJOR_VERSION;
	private static final int MINOR_VERSION;
	private static final int REVISION_NUMBER;

	static {
		String craftBukkitPackage = Bukkit.getServer().getClass().getPackage().getName();
		Matcher matcher = VERSION_PATTERN.matcher(craftBukkitPackage);

		if (!matcher.find()) {
			throw new RuntimeException("Can't parse craftbukkit package version " + craftBukkitPackage);
		}

		NMS_VERSION = matcher.group(1);
		MAJOR_VERSION = Integer.parseInt(matcher.group(2));
		MINOR_VERSION = Integer.parseInt(matcher.group(3));
		REVISION_NUMBER = Integer.parseInt(matcher.group(4));
	}

	public static String nmsVersion() {
		return NMS_VERSION;
	}

	public static int majorVersion() {
		return MAJOR_VERSION;
	}

	public static int minorVersion() {
		return MINOR_VERSION;
	}

	public static int revisionNumber() {
		return REVISION_NUMBER;
	}

	private MinecraftVersion() {
	}
}
