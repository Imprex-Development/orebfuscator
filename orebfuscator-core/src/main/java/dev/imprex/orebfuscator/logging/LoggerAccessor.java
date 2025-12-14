package dev.imprex.orebfuscator.logging;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface LoggerAccessor {

  void log(LogLevel level, String message, @Nullable Throwable throwable);

}