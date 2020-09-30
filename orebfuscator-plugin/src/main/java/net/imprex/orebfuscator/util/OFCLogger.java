package net.imprex.orebfuscator.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class OFCLogger {

	public static final Logger LOGGER = Logger.getLogger("bukkit.orebfuscator");
	private static final String LOG_PREFIX = "[Orebfuscator] ";

	public static void warn(String message) {
		log(Level.WARNING, message);
	}

	/**
	 * Log an information
	 */
	public static void info(String message) {
		log(Level.INFO, message);
	}

	/**
	 * Log with a specified level
	 */
	public static void log(Level level, String message) {
		OFCLogger.LOGGER.log(level, LOG_PREFIX + message);
	}

	/**
	 * Log an error
	 */
	public static void err(Throwable e) {
		log(Level.SEVERE, e.getMessage(), e);
	}

	/**
	 * Log with a specified level and throwable
	 */
	public static void log(Level level, String message, Throwable throwable) {
		OFCLogger.LOGGER.log(level, LOG_PREFIX + message, throwable);
	}
}