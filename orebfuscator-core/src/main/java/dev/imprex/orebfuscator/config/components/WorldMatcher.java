package dev.imprex.orebfuscator.config.components;

import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

public class WorldMatcher implements Predicate<String> {

  @NotNull
  public static WorldMatcher parseMatcher(@NotNull String value) {
    var parsed = ConfigFunctionValue.parse(value);
    if (parsed != null && parsed.function().equals("regex")) {
      return new WorldMatcher(parseRegexMatcher(parsed.argument()), Type.REGEX);
    } else {
      return new WorldMatcher(parseWildcardMatcher(value), Type.WILDCARD);
    }
  }

  private static Pattern parseRegexMatcher(String pattern) {
    return Pattern.compile(pattern);
  }

  private static Pattern parseWildcardMatcher(String value) {
    String pattern = ("\\Q" + value + "\\E").replace("*", "\\E.*\\Q");
    return Pattern.compile(pattern);
  }

  private final Pattern pattern;
  private final Type type;

  private WorldMatcher(Pattern pattern, Type type) {
    this.pattern = pattern;
    this.type = type;
  }

  @Override
  public boolean test(String value) {
    return this.pattern.matcher(value).matches();
  }

  public String serialize() {
    if (this.type == Type.REGEX) {
      return String.format("regex(%s)", this.pattern.pattern());
    } else {
      return this.pattern.pattern()
          .replace("\\E.*\\Q", "*")
          .replaceAll("\\\\Q|\\\\E", "");
    }
  }

  private enum Type {
    REGEX, WILDCARD
  }
}
