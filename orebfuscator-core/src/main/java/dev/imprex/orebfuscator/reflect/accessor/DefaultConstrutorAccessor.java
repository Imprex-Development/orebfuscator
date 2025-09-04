package dev.imprex.orebfuscator.reflect.accessor;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;

record DefaultConstructorAccessor(@NotNull Constructor<?> member, @NotNull MethodHandle methodHandle) implements
    ConstructorAccessor {

  @Override
  public Object invoke(Object... args) {
    try {
      return methodHandle.invokeExact(args);
    } catch (Throwable throwable) {
      throw new IllegalStateException("Unable to construct new instance using " + member, throwable);
    }
  }

  @Override
  public @NotNull Constructor<?> member() {
    return member;
  }
}
