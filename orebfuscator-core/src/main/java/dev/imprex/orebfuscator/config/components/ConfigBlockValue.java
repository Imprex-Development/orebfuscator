package dev.imprex.orebfuscator.config.components;

import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import dev.imprex.orebfuscator.util.BlockProperties;
import dev.imprex.orebfuscator.util.BlockTag;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record ConfigBlockValue(String value, Set<BlockProperties> blocks) implements
    Comparable<ConfigBlockValue> {

  public static void dump(ConfigurationSection section, Collection<? extends ConfigBlockValue> values) {
    for (var entry : values.stream().sorted().toList()) {
      if (entry.blocks().size() > 1) {
        section.set(entry.value(), entry.blocks().stream()
            .map(block -> block.getKey().toString())
            .toList());
      } else if (!entry.blocks().isEmpty()) {
        section.set(entry.value(), "valid");
      } else {
        section.set(entry.value(), "invalid");
      }
    }
  }

  public static ConfigBlockValue invalid(String value) {
    return new ConfigBlockValue(value, Collections.emptySet());
  }

  public static ConfigBlockValue block(BlockProperties block) {
    return new ConfigBlockValue(block.getKey().toString(), Set.of(block));
  }

  public static ConfigBlockValue invalidTag(String value) {
    return new ConfigBlockValue(String.format("tag(%s)", value), Collections.emptySet());
  }

  public static ConfigBlockValue tag(BlockTag tag, Set<BlockProperties> blocks) {
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
