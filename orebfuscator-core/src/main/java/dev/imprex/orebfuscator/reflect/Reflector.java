package dev.imprex.orebfuscator.reflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import dev.imprex.orebfuscator.reflect.accessor.Accessors;
import dev.imprex.orebfuscator.reflect.accessor.ConstructorAccessor;
import dev.imprex.orebfuscator.reflect.accessor.FieldAccessor;
import dev.imprex.orebfuscator.reflect.accessor.MethodAccessor;
import dev.imprex.orebfuscator.reflect.predicate.ConstructorPredicate;
import dev.imprex.orebfuscator.reflect.predicate.FieldPredicate;
import dev.imprex.orebfuscator.reflect.predicate.MethodPredicate;

public class Reflector {

  public static Reflector of(Class<?> target) {
    return new Reflector(target);
  }

  private final @NotNull Class<?> target;
  private @NotNull Class<?> recursiveUntil;

  private Reflector(@NotNull Class<?> target) {
    this.target = Objects.requireNonNull(target);
    this.recursiveUntil = target;
  }

  public Reflector recursive() {
    return this.recursiveUntil(Object.class);
  }

  public Reflector recursiveUntil(Class<?> recursiveUntil) {
    this.recursiveUntil = recursiveUntil != null
        ? recursiveUntil
        : this.target;
    return this;
  }

  private String className() {
    if (target == recursiveUntil) {
      return target.getTypeName();
    } else if (recursiveUntil == Object.class) {
      return String.format("%s recursively", target.getTypeName());
    } else {
      return String.format("%s recursively until %s", this.target.getTypeName(),
          this.recursiveUntil.getTypeName());
    }
  }

  private <T> Stream<T> get(Function<Class<?>, T[]> getter) {
    Class<?> current = this.target;
    Stream<T> stream = Stream.empty();

    while (current != null) {
      stream = Stream.concat(stream, Arrays.stream(getter.apply(current)));
      if (current == recursiveUntil) {
        break;
      }
      current = current.getSuperclass();
    }

    return stream;
  }

  @NotNull
  public Stream<ConstructorAccessor> constructor(Predicate<Constructor<?>> predicate) {
    Stream<Constructor<?>> stream = get(Class::getDeclaredConstructors);
    return stream.filter(predicate).map(Accessors::wrap);
  }

  @NotNull
  public ConstructorPredicate constructor() {
    return new ConstructorPredicate(this::constructor, this::className);
  }

  @NotNull
  public Stream<FieldAccessor> field(Predicate<Field> predicate) {
    Stream<Field> stream = get(Class::getDeclaredFields);
    return stream.filter(predicate).map(Accessors::wrap);
  }

  @NotNull
  public FieldPredicate field() {
    return new FieldPredicate(this::field, this::className);
  }

  @NotNull
  public Stream<MethodAccessor> method(Predicate<Method> predicate) {
    Stream<Method> stream = get(Class::getDeclaredMethods);
    return stream.filter(predicate).map(Accessors::wrap);
  }

  @NotNull
  public MethodPredicate method() {
    return new MethodPredicate(this::method, this::className);
  }
}
