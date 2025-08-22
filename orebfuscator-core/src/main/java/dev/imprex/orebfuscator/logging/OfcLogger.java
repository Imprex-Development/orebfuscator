package dev.imprex.orebfuscator.logging;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class OfcLogger {

	public static Logger LOGGER = Logger.getLogger("Orebfuscator");

	private static final Queue<String> VERBOSE_LOG = new LinkedList<String>();
	private static boolean verbose = false;

	public static void setVerboseLogging(boolean verbose) {
		OfcLogger.verbose = verbose;
		if (OfcLogger.verbose) {
			debug("Verbose logging has been enabled");
		}
	}

	public static void debug(String message) {
		if (OfcLogger.verbose) {
			OfcLogger.LOGGER.log(Level.INFO, "[Debug] " + message);
		}

		synchronized (VERBOSE_LOG) {
			while (VERBOSE_LOG.size() >= 1000) {
				VERBOSE_LOG.poll();
			}
			VERBOSE_LOG.offer(message);
		}
	}

	public static String getLatestVerboseLog() {
		synchronized (VERBOSE_LOG) {
			int length = 0;
			for (String message : VERBOSE_LOG) {
				length += message.length() + 1;
			}

			StringBuilder builder = new StringBuilder(length);
			for (String message : VERBOSE_LOG) {
				builder.append(message).append("\n");
			}
			builder.deleteCharAt(builder.length() - 1);
			return builder.toString();
		}
	}

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
		OfcLogger.LOGGER.log(level, message);
	}

	/**
	 * Log an error
	 */
	public static void error(Throwable e) {
		log(Level.SEVERE, e.getMessage(), e);
	}

	/**
	 * Log an error
	 */
	public static void error(String message, Throwable e) {
		log(Level.SEVERE, message, e);
	}

	/**
	 * Log with a specified level and throwable
	 */
	public static void log(Level level, String message, Throwable throwable) {
		OfcLogger.LOGGER.log(level, message, throwable);
	}
}