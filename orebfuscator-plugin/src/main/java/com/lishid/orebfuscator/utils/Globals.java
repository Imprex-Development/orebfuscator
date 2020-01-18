package com.lishid.orebfuscator.utils;

import java.util.regex.Pattern;

import org.bukkit.Bukkit;

public class Globals {

	public static final Pattern NMS_PATTERN = Pattern.compile("v(\\d+)_(\\d+)_R\\d");

	public static final String SERVER_VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
	public static final String LOG_PREFIX = "[OFC] ";

	public static final int MASK_OBFUSCATE = 1;
	public static final int MASK_TILEENTITY = 2;
	public static final int MASK_PROXIMITYHIDER = 4;
	public static final int MASK_DARKNESSBLOCK = 8;
}
