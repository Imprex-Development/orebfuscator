package net.imprex.orebfuscator.iterop;

import java.util.Objects;
import java.util.logging.Logger;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.imprex.orebfuscator.logging.LogLevel;
import dev.imprex.orebfuscator.logging.LoggerAccessor;

public class BukkitLoggerAccessor implements LoggerAccessor {

	private final Logger logger;

	public BukkitLoggerAccessor(@NotNull Logger logger) {
		this.logger = Objects.requireNonNull(logger, "Plugin logger can't be null");
	}

	@Override
	public void log(@NotNull LogLevel level, @NotNull String message, @Nullable Throwable throwable) {
		var mappedLevel = switch (level) {
			case DEBUG, INFO -> java.util.logging.Level.INFO;
	        case WARN -> java.util.logging.Level.WARNING;
			case ERROR -> java.util.logging.Level.SEVERE;
		};
	
		if (level == LogLevel.DEBUG) {
			message = "[Debug] " + message;
		}
	
		this.logger.log(mappedLevel, message, throwable);
	}
}
