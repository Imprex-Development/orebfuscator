package dev.imprex.orebfuscator.logging;

import java.io.PrintStream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SystemLogger implements LoggerAccessor {

  @Override
  public void log(@NotNull LogLevel level, @NotNull String message, @Nullable Throwable throwable) {
    PrintStream stream = level == LogLevel.ERROR ? System.err : System.out;
    stream.printf("[Orebfuscator - %s] %s%n", level, message);

    if (throwable != null) {
      throwable.printStackTrace(stream);
    }
  }
}
