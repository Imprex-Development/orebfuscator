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
	 * Log an error
	 */
	public static void err(String message, Throwable throwable) {
		log(Level.SEVERE, message, throwable);
	}

	/**
	 * Log with a specified level
	 */
	@Deprecated
	public static void log(Level level, String message) {
		OFCLogger.LOGGER.log(level, LOG_PREFIX + message);
	}

	/**
	 * Log with a specified level and throwable
	 */
	@Deprecated
	public static void log(Level level, String message, Throwable throwable) {
		OFCLogger.LOGGER.log(level, LOG_PREFIX + message, throwable);
	}
}