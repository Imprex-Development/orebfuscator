package dev.imprex.orebfuscator.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LoggerAccessor {

  void log(@NotNull LogLevel level, @NotNull String message, @Nullable Throwable throwable);

}