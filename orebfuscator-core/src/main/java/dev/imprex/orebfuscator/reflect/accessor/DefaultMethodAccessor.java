package dev.imprex.orebfuscator.reflect.accessor;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;

record DefaultMethodAccessor(@NonNull Method member, @NonNull MethodHandle methodHandle) implements
    MethodAccessor {

  @Override
  public Object invoke(Object instance, Object... args) {
    try {
      return methodHandle.invokeExact(instance, args);
    } catch (Throwable throwable) {
      throw new IllegalStateException("Unable to invoke method " + member, throwable);
    }
  }

  @Override
  public @NonNull Method member() {
    return member;
  }
}
