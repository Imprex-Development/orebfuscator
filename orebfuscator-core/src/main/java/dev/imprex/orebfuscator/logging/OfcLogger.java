package dev.imprex.orebfuscator.logging;

import dev.imprex.orebfuscator.util.QuickMaths;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class OfcLogger {

  private static LoggerAccessor logger = new SystemLogger();
  private static boolean verbose;

  private static final Queue<String> MESSAGE_LOG = new ConcurrentLinkedQueue<>();
  private static final Map<String, AtomicLong> COUNTERS = new ConcurrentHashMap<>();

  public static void setLogger(LoggerAccessor logger) {
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

  public static String getLatestLog() {
    return String.join("\n", MESSAGE_LOG);
  }

  public static void debug(String message) {
    log(LogLevel.DEBUG, message);
  }

  public static void info(String message) {
    log(LogLevel.INFO, message);
  }

  public static void warn(String message) {
    log(LogLevel.WARN, message);
  }

  public static void error(Throwable throwable) {
    log(LogLevel.ERROR, "An error occurred:", throwable);
  }

  public static void error(String message, @Nullable Throwable throwable) {
    log(LogLevel.ERROR, message, throwable);
  }

  public static void throttle(LogLevel level, String message) {
    var count = COUNTERS.computeIfAbsent(message, k -> new AtomicLong()).incrementAndGet();
    if (count < 16) {
      log(level, message);
    } else if (QuickMaths.isPowerOfTwo(count)) {
      log(level, "[x%d] %s".formatted(count, message));
    }
  }

  public static void log(LogLevel level, String message) {
    log(level, message, null);
  }

  public static void log(LogLevel level, String message, @Nullable Throwable throwable) {
    Objects.requireNonNull(level);
    Objects.requireNonNull(message);

    while (MESSAGE_LOG.size() >= 2048) {
      MESSAGE_LOG.poll();
    }
    MESSAGE_LOG.offer(message);

    // filter out debug if verbose logging is disabled
    if (level == LogLevel.DEBUG && !verbose) {
      return;
    }

    logger.log(level, message, throwable);
  }
}
