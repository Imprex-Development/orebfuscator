package dev.imprex.orebfuscator.statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
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

  public JsonObject json() {
    JsonObject object = new JsonObject();

    for (var entry : sources.entrySet()) {
      object.add(entry.getKey(), entry.getValue().json());
    }

    return object;
  }
}
