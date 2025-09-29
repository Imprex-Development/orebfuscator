package dev.imprex.orebfuscator.config.components;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record ConfigFunctionValue(@NotNull String function, @NotNull String argument) {

  private static final Pattern CONFIG_FUNCTION_PATTERN = Pattern.compile("^(?<function>\\w+)\\((?<argument>.+)\\)$");

  @Nullable
  public static ConfigFunctionValue parse(@NotNull String value) {
    Matcher matcher = CONFIG_FUNCTION_PATTERN.matcher(value);
    if (matcher.find()) {
      String function = matcher.group("function");
      String argument = matcher.group("argument");

      return new ConfigFunctionValue(function.toLowerCase(), argument);
    } else {
      return null;
    }
  }
}
