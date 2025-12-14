package net.imprex.orebfuscator.iterop;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;


import dev.imprex.orebfuscator.logging.LogLevel;
import dev.imprex.orebfuscator.logging.LoggerAccessor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record BukkitLoggerAccessor(Logger logger) implements LoggerAccessor {

  public BukkitLoggerAccessor {
    Objects.requireNonNull(logger, "Plugin logger can't be null");
  }

  @Override
  public void log(LogLevel level, String message, @Nullable Throwable throwable) {
    var mappedLevel = switch (level) {
      case DEBUG, INFO -> Level.INFO;
      case WARN -> Level.WARNING;
      case ERROR -> Level.SEVERE;
    };

    if (level == LogLevel.DEBUG) {
      message = "[Debug] " + message;
    }

    this.logger.log(mappedLevel, message, throwable);
  }
}
