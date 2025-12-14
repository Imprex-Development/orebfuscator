package dev.imprex.orebfuscator.reflect.accessor;


import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import org.jspecify.annotations.NonNull;

record DefaultConstructorAccessor(@NonNull Constructor<?> member, @NonNull MethodHandle methodHandle) implements
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
  public @NonNull Constructor<?> member() {
    return member;
  }
}
