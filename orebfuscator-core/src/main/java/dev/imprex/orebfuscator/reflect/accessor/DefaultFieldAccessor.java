package dev.imprex.orebfuscator.reflect.accessor;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final record DefaultFieldAccessor(@NotNull Field member, @NotNull MethodHandle getterHandle, @Nullable MethodHandle setterHandle) implements FieldAccessor {

  @Override
  public boolean readonly() {
    return setterHandle == null;
  }

  @Override
  public Object get(Object instance) {
    try {
      return getterHandle.invokeExact(instance);
    } catch (Throwable throwable) {
      throw new IllegalStateException("Unable to get field value of " + member, throwable);
    }
  }

  @Override
  public void set(Object instance, Object value) {
    if (readonly()) {
      throw new IllegalStateException("Can't set value of trusted final field " + member);
    }

    try {
      setterHandle.invokeExact(instance, value);
    } catch (Throwable throwable) {
      throw new IllegalStateException("Unable to set value of field " + member, throwable);
    }
  }

  @Override
  public @NotNull Field member() {
    return this.member;
  }
}
