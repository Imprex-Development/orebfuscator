package dev.imprex.orebfuscator.statistics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import com.google.gson.JsonObject;

public class StatisticsRegistry {

  private final Map<String, StatisticsSource> sources = new HashMap<>();

  public void register(String name, StatisticsSource source) {
    if (sources.containsKey(name)) {
      throw new IllegalArgumentException("Duplicate statistics name: " + name);
    }

    this.sources.put(name, source);
  }

  public String format() {
    var joiner = new StringJoiner("\n", "Here are some useful statistics:\n", "");

    for (var source : sources.values()) {
      source.add(joiner);
    }

    return joiner.toString();
  }

  public String debug() {
    return entries().stream()
        .sorted(Comparator.comparing(Map.Entry::getKey))
        .map(entry -> String.format(" - %s: %s", entry.getKey(), entry.getValue()))
        .collect(Collectors.joining("\n"));
  }

  public List<Map.Entry<String, String>> entries() {
    var entries = new ArrayList<Map.Entry<String, String>>();

    for (var source : sources.values()) {
      source.debug(entries::add);
    }

    return entries;
  }
}
