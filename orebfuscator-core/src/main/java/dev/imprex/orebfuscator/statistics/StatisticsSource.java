package dev.imprex.orebfuscator.statistics;

import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Consumer;

public interface StatisticsSource {

  default String faction(double value) {
    return String.format("%.3f", value);
  }

  default String percent(double value) {
    return String.format("%.2f%%", value * 100);
  }

  default String time(long time) {
    if (time > 1000_000L) {
      return String.format("%.1fms", time / 1000_000d);
    } else if (time > 1000L) {
      return String.format("%.1fµs", time / 1000d);
    } else {
      return String.format("%dns", time);
    }
  }

  default String bytes(long bytes) {
    if (bytes > 1073741824L) {
      return String.format("%.1f GiB", bytes / 1073741824d);
    } else if (bytes > 1048576L) {
      return String.format("%.1f MiB", bytes / 1048576d);
    } else if (bytes > 1024L) {
      return String.format("%.1f KiB", bytes / 1024d);
    } else {
      return String.format("%d B", bytes);
    }
  }

  void add(StringJoiner joiner);

  void debug(Consumer<Map.Entry<String, String>> consumer);
}
