package dev.imprex.orebfuscator.reflect.predicate;

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

class RequirementCollector {

  private final StringBuilder builder;

  public RequirementCollector(@NotNull String prefix) {
    builder = new StringBuilder(prefix).append("{\n");
  }

  @NotNull
  public RequirementCollector collect(@NotNull String name) {
    Objects.requireNonNull(name);

    builder.append("  ").append(name).append(",\n");
    return this;
  }

  @NotNull
  public RequirementCollector collect(@NotNull String name, @NotNull Object value) {
    Objects.requireNonNull(name);
    Objects.requireNonNull(value);

    builder.append("  ").append(name).append(": ").append(value).append(",\n");
    return this;
  }

  @NotNull
  public String get() {
    return builder.delete(builder.length() - 2, builder.length()).append("\n}").toString();
  }
}
