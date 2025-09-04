package dev.imprex.orebfuscator.reflect.predicate;

import java.util.Objects;
import java.util.StringJoiner;
import org.jetbrains.annotations.NotNull;

class RequirementCollector {

  private final StringJoiner entries;

  public RequirementCollector(@NotNull String prefix) {
    this.entries = new StringJoiner(",\n", prefix + "{\n", "\n}");
  }

  @NotNull
  public RequirementCollector collect(@NotNull String name) {
    Objects.requireNonNull(name);

    entries.add("  " + name);
    return this;
  }

  @NotNull
  public RequirementCollector collect(@NotNull String name, @NotNull Object value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);

    entries.add("  " + name + ": " + value);
    return this;
  }

  @NotNull
  public String get() {
    return entries.toString();
  }
}
