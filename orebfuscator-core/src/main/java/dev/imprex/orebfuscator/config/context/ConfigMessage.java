package dev.imprex.orebfuscator.config.context;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfigMessage {

  public static final ConfigMessage MISSING_OR_EMPTY = fixed("is missing or empty");
  public static final ConfigMessage MISSING_USING_DEFAULTS = fixed("is missing, using defaults");

  public static final ConfigMessage DISABLED_ERRORS = fixed("got disabled due to errors");

  public static final ConfigMessage VALUE_MIN = dynamic("value too low {value(%d) < min(%d)}");
  public static final ConfigMessage VALUE_MIN_MAX = dynamic("value out of range {value(%d) not in range[%d, %d]}");

  public static final ConfigMessage FUNCTION_UNKNOWN = dynamic(
      "skipping unknown function literal '%s(...)' with argument '%s'");

  public static final ConfigMessage CACHE_INVALID_PATH = dynamic(
      "contains malformed path '%s', using default path '%s'");
  public static final ConfigMessage CACHE_CANT_CREATE = dynamic("can't create cache directory '%s'");

  public static final ConfigMessage BLOCK_TAG_UNKNOWN = dynamic("skipping unknown block tag '%s'");
  public static final ConfigMessage BLOCK_TAG_EMPTY = dynamic("skipping empty block tag '%s'");
  public static final ConfigMessage BLOCK_TAG_AIR_BLOCK = dynamic("skipping air block '%s' for block tag '%s'");
  public static final ConfigMessage BLOCK_TAG_AIR_ONLY = dynamic(
      "skipping block tag '%s' because it only contains air");

  public static final ConfigMessage BLOCK_UNKNOWN = dynamic("skipping unknown block '%s'");
  public static final ConfigMessage BLOCK_AIR = dynamic("skipping air block '%s'");

  private static ConfigMessage fixed(@NotNull String message) {
    return new ConfigMessage(message, true);
  }

  private static ConfigMessage dynamic(@NotNull String message) {
    return new ConfigMessage(message, false);
  }

  private final String message;
  private final boolean fixed;

  private ConfigMessage(@NotNull String message, boolean fixed) {
    this.message = message;
    this.fixed = fixed;
  }

  @NotNull
  @Contract(pure = true)
  public String format(@Nullable Object... arguments) {
    return this.fixed
        ? this.message
        : String.format(this.message, arguments);
  }
}
