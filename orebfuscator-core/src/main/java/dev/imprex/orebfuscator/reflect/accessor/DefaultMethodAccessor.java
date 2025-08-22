package dev.imprex.orebfuscator.reflect.accessor;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import org.jetbrains.annotations.NotNull;

final record DefaultMethodAccessor(@NotNull Method member, @NotNull MethodHandle methodHandle) implements MethodAccessor {

  @Override
  public Object invoke(Object instance, Object... args) {
    try {
      return methodHandle.invokeExact(instance, args);
    } catch (Throwable throwable) {
      throw new IllegalStateException("Unable to invoke method " + member, throwable);
    }
  }

  @Override
  public @NotNull Method member() {
    return member;
  }
}
