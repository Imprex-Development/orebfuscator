package dev.imprex.orebfuscator.logging;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OfcLogger {

  private static LoggerAccessor logger = new SystemLogger();

  private static final Queue<String> VERBOSE_LOG = new ConcurrentLinkedQueue<>();
  private static boolean verbose;

  public static void setLogger(@NotNull LoggerAccessor logger) {
    if (OfcLogger.logger instanceof SystemLogger) {
      OfcLogger.logger = Objects.requireNonNull(logger);
    }
  }

  public static void setVerboseLogging(boolean enabled) {
    if (!verbose && enabled) {
      verbose = true;
      debug("Verbose logging has been enabled");
    } else {
      verbose = enabled;
    }
  }

  @NotNull
  public static String getLatestVerboseLog() {
    return String.join("\n", VERBOSE_LOG);
  }

  public static void debug(@NotNull String message) {
    log(LogLevel.DEBUG, message);
  }

  public static void info(@NotNull String message) {
    log(LogLevel.INFO, message);
  }

  public static void warn(@NotNull String message) {
    log(LogLevel.WARN, message);
  }

  public static void error(@NotNull Throwable throwable) {
    log(LogLevel.ERROR, "An error occurred:", throwable);
  }

  public static void error(@NotNull String message, @Nullable Throwable throwable) {
    log(LogLevel.ERROR, message, throwable);
  }

  public static void log(@NotNull LogLevel level, @NotNull String message) {
    log(level, message, null);
  }

  public static void log(@NotNull LogLevel level, @NotNull String message, @Nullable Throwable throwable) {
    Objects.requireNonNull(level);
    Objects.requireNonNull(message);

    if (level == LogLevel.DEBUG) {
      // always store debug messages for system dumps
      while (VERBOSE_LOG.size() >= 1000) {
        VERBOSE_LOG.poll();
      }

      VERBOSE_LOG.offer(message);

      // filter out debug if verbose logging is disabled
      if (!verbose) {
        return;
      }
    }

    logger.log(level, message, throwable);
  }
}
