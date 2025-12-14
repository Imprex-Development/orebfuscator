package dev.imprex.orebfuscator.logging;

import java.io.PrintStream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class SystemLogger implements LoggerAccessor {

  @Override
  public void log(LogLevel level, String message, @Nullable Throwable throwable) {
    PrintStream stream = level == LogLevel.ERROR ? System.err : System.out;
    stream.printf("[Orebfuscator - %s] %s%n", level, message);

    if (throwable != null) {
      throwable.printStackTrace(stream);
    }
  }
}
