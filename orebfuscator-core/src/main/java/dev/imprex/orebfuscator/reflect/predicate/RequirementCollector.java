package dev.imprex.orebfuscator.reflect.predicate;

import java.util.Objects;
import java.util.StringJoiner;
import org.jspecify.annotations.NullMarked;

@NullMarked
class RequirementCollector {

  private final StringJoiner entries;

  public RequirementCollector(String prefix) {
    this.entries = new StringJoiner(",\n", prefix + "{\n", "\n}");
  }

  public RequirementCollector collect(String name) {
    Objects.requireNonNull(name);

    entries.add("  " + name);
    return this;
  }

  public RequirementCollector collect(String name, Object value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);

    entries.add("  " + name + ": " + value);
    return this;
  }

  public String get() {
    return entries.toString();
  }
}
