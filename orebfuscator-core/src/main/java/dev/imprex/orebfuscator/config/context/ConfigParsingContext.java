package dev.imprex.orebfuscator.config.context;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a context used during configuration parsing to track and collect warnings and errors.
 * <p>
 * Paths within this context are always '.' separated and cannot contain only whitespace. A path is always relative to
 * the current context and cannot reference a parent context ('..').
 * </p>
 * <p>
 * If a child section isolates errors, the parent context will not return true for {@code hasErrors()}, even if the
 * child section or its descendants contain errors. This allows certain configuration sections to be disabled
 * individually without affecting the entire configuration.
 * </p>
 */
public interface ConfigParsingContext {

  /**
   * Creates or retrieves an existing child context for the specified path.
   *
   * @param path          The relative path of the child context.
   * @param isolateErrors Whether errors in this child context should be isolated from the parent.
   * @return An existing or newly created {@code ConfigParsingContext} instance for the given path.
   */
  @NotNull
  ConfigParsingContext section(@NotNull String path, boolean isolateErrors);

  /**
   * Creates or retrieves an existing child context for the specified path without isolating errors.
   *
   * @param path The relative path of the child context.
   * @return An existing or newly created {@code ConfigParsingContext} instance for the given path.
   */
  @NotNull
  default ConfigParsingContext section(@NotNull String path) {
    return section(path, false);
  }

  /**
   * Adds a warning message to the current context.
   *
   * @param message   The warning message to be logged.
   * @param arguments Optional arguments used to format the message.
   */
  void warn(@NotNull ConfigMessage message, @Nullable Object... arguments);

  /**
   * Adds a warning message to the context of the specified path.
   *
   * @param path      The relative path where the warning should be logged.
   * @param message   The warning message to be logged.
   * @param arguments Optional arguments used to format the message.
   */
  void warn(@NotNull String path, @NotNull ConfigMessage message, @Nullable Object... arguments);

  /**
   * Determines whether a subsystem should remain enabled based on the presence of errors.
   * <p>
   * If errors are present in this context, the subsystem should be disabled. Otherwise, the given enabled state is
   * returned unchanged.
   * </p>
   *
   * @param enabled The current enabled state of the subsystem.
   * @return {@code false} if errors are present and the subsystem would be enabled, otherwise returns the given
   * {@code enabled} state.
   * @see #hasErrors()
   */
  default boolean disableIfError(boolean enabled) {
    if (enabled && hasErrors()) {
      warn(ConfigMessage.DISABLED_ERRORS);
      return false;
    }
    return enabled;
  }

  /**
   * Adds an error message to the current context.
   *
   * @param message   The error message to be logged.
   * @param arguments Optional arguments used to format the message.
   */
  void error(@NotNull ConfigMessage message, @Nullable Object... arguments);

  /**
   * Adds an error message to the context of the specified path.
   *
   * @param path      The relative path where the error should be logged.
   * @param message   The error message to be logged.
   * @param arguments Optional arguments used to format the message.
   */
  void error(@NotNull String path, @NotNull ConfigMessage message, @Nullable Object... arguments);

  /**
   * Adds an error if the specified value is below the minimum allowed value.
   *
   * @param path  The relative path where the error should be logged.
   * @param min   The minimum allowed value.
   * @param value The actual value to be checked.
   */
  default void errorMinValue(@NotNull String path, long min, long value) {
    if (value < min) {
      error(path, ConfigMessage.VALUE_MIN, value, min);
    }
  }

  /**
   * Adds an error if the specified value is outside the allowed min-max range (inclusive).
   *
   * @param path  The relative path where the error should be logged.
   * @param min   The minimum allowed value.
   * @param max   The maximum allowed value.
   * @param value The actual value to be checked.
   */
  default void errorMinMaxValue(@NotNull String path, long min, long max, long value) {
    if (value < min || value > max) {
      error(path, ConfigMessage.VALUE_MIN_MAX, value, min, max);
    }
  }

  /**
   * Checks whether this context or any of its child contexts (recursively) contain errors.
   * <p>
   * If a child context is set to isolate errors, its errors will not contribute to the result of this method. This
   * allows for selective error handling within different parts of the configuration.
   * </p>
   *
   * @return {@code true} if errors are present in this context or any non-isolating child contexts, otherwise
   * {@code false}.
   */
  @Contract(pure = true)
  boolean hasErrors();
}
