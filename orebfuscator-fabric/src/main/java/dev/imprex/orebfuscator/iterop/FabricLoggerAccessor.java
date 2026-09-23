package dev.imprex.orebfuscator.iterop;

import dev.imprex.orebfuscator.logging.LogLevel;
import dev.imprex.orebfuscator.logging.LoggerAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FabricLoggerAccessor implements LoggerAccessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(FabricLoggerAccessor.class);

  @Override
  public void log(@NotNull LogLevel level, @NotNull String message, @Nullable Throwable throwable) {
    if (level == LogLevel.DEBUG) {
      LOGGER.info(message, throwable);
    } else if (level == LogLevel.WARN) {
      LOGGER.warn(message, throwable);
    } else if (level == LogLevel.ERROR) {
      LOGGER.error(message, throwable);
    } else { // INFO or fallback
      LOGGER.info(message, throwable);
    }
  }
}
