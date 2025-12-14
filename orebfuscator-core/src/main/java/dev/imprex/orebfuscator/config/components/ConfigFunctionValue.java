package dev.imprex.orebfuscator.config.components;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record ConfigFunctionValue(String function, String argument) {

  private static final Pattern CONFIG_FUNCTION_PATTERN = Pattern.compile("^(?<function>\\w+)\\((?<argument>.+)\\)$");

  public static @Nullable ConfigFunctionValue parse(String value) {
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
