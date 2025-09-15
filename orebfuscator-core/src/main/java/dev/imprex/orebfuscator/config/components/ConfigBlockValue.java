package dev.imprex.orebfuscator.config.components;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import dev.imprex.orebfuscator.util.BlockProperties;
import dev.imprex.orebfuscator.util.BlockTag;

public record ConfigBlockValue(@NotNull String value, @NotNull Set<BlockProperties> blocks) implements
    Comparable<ConfigBlockValue> {

  public static JsonObject toJson(Collection<? extends ConfigBlockValue> values) {
    JsonObject object = new JsonObject();

    var list = values.stream().sorted(Comparator.comparing((ConfigBlockValue a) -> a.value())).toList();

    for (var entry : list) {
      if (entry.blocks().size() > 1) {
        JsonArray array = new JsonArray(entry.blocks().size());
        for (var block : entry.blocks()) {
          array.add(block.getKey().toString());
        }
        object.add(entry.value(), array);
      } else {
        object.add(entry.value(), JsonNull.INSTANCE);
      }
    }

    return object;
  }

  @NotNull
  public static ConfigBlockValue invalid(@NotNull String value) {
    return new ConfigBlockValue(value, Collections.emptySet());
  }

  @NotNull
  public static ConfigBlockValue block(@NotNull BlockProperties block) {
    return new ConfigBlockValue(block.getKey().toString(), Set.of(block));
  }

  @NotNull
  public static ConfigBlockValue invalidTag(@NotNull String value) {
    return new ConfigBlockValue(String.format("tag(%s)", value), Collections.emptySet());
  }

  @NotNull
  public static ConfigBlockValue tag(@NotNull BlockTag tag, @NotNull Set<BlockProperties> blocks) {
    return new ConfigBlockValue(String.format("tag(%s)", tag.key()), Collections.unmodifiableSet(blocks));
  }

  @Override
  public int hashCode() {
    return this.value.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (this == obj) || (obj instanceof ConfigBlockValue other) && Objects.equals(this.value, other.value);
  }

  @Override
  public int compareTo(ConfigBlockValue o) {
    boolean isATag = this.value().startsWith("tag(");
    boolean isBTag = o.value().startsWith("tag(");

    int tag = Boolean.compare(isATag, isBTag);
    if (tag == 0) {
      return this.value().compareTo(o.value());
    }

    return tag;
  }
}
