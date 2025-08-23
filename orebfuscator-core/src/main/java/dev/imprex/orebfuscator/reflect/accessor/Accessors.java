package dev.imprex.orebfuscator.reflect.accessor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public final class Accessors {

  private static final Lookup LOOKUP = MethodHandles.lookup();

  private static final MethodType FIELD_GETTER = MethodType.methodType(Object.class, Object.class);
  private static final MethodType FIELD_SETTER = MethodType.methodType(void.class, Object.class, Object.class);

  private Accessors() {}

  public static @NotNull ConstructorAccessor wrap(@NotNull Constructor<?> constructor) {
    return create(constructor, () -> {
      MethodHandle methodHandle = LOOKUP.unreflectConstructor(constructor);
      methodHandle = generifyExecutable(methodHandle, false, true);

      return new DefaultConstructorAccessor(constructor, methodHandle);
    });
  }

  public static @NotNull FieldAccessor wrap(@NotNull Field field) {
    return create(field, () -> {
      MethodHandle getter = LOOKUP.unreflectGetter(field);
      MethodHandle setter = null;
      
      try {
        setter = LOOKUP.unreflectSetter(field);
      } catch (IllegalAccessException e) {
      }

      if (Modifier.isStatic(field.getModifiers())) {
        getter = MethodHandles.dropArguments(getter, 0, Object.class);
        setter = setter != null ? MethodHandles.dropArguments(setter, 0, Object.class) : null;
      }

      getter = getter.asType(FIELD_GETTER);
      setter = setter != null ? setter.asType(FIELD_SETTER) : null;

      return new DefaultFieldAccessor(field, getter, setter);
    });
  }

  public static @NotNull MethodAccessor wrap(@NotNull Method method) {
    return create(method, () -> {
      MethodHandle methodHandle = LOOKUP.unreflect(method);
      methodHandle = generifyExecutable(methodHandle, Modifier.isStatic(method.getModifiers()), false);

      return new DefaultMethodAccessor(method, methodHandle);
    });
  }

  private static <TMember extends AccessibleObject & Member, TAccessor> @NotNull TAccessor create(
      @NotNull TMember member, @NotNull AccessorFactory<TAccessor> factory) {
    Objects.requireNonNull(member);

    @SuppressWarnings("deprecation")
    boolean accessible = member.isAccessible();
    try {
      member.setAccessible(true);

      return factory.create();
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Unable to create accessor for " + member, e);
    } finally {
      member.setAccessible(accessible);
    }
  }

  private static MethodHandle generifyExecutable(MethodHandle handle, boolean isStatic, boolean isConstructor) {
    // force the method to use a fixed arity, ensuring it accepts varargs directly as an array
    MethodHandle target = handle.asFixedArity();

    // determine the number of parameters to spread: subtract 1 for instance methods, as the first parameter is the
    // receiver (the instance object)
    int paramCount = handle.type().parameterCount() - (isConstructor || isStatic ? 0 : 1);

    // spread the Object[] arguments into individual parameters (after the instance object for non-static methods)
    target = target.asSpreader(Object[].class, paramCount);

    if (isStatic) {
      // add a dummy instance parameter at the beginning for static methods to unify the calling convention with
      // instance methods
      target = MethodHandles.dropArguments(target, 0, Object.class);
    }

    // convert the MethodHandle to a generic signature:
    // return Object, take Object instance (receiver) if needed, and an Object[] for the remaining arguments
    return target.asType(MethodType.genericMethodType(isConstructor ? 0 : 1, true));
  }

  private interface AccessorFactory<T> {
    T create() throws IllegalAccessException;
  }
}
