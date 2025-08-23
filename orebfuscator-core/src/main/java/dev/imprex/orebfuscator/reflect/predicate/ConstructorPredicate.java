package dev.imprex.orebfuscator.reflect.predicate;

import java.lang.reflect.Constructor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import dev.imprex.orebfuscator.reflect.accessor.ConstructorAccessor;

public final class ConstructorPredicate
    extends AbstractExecutablePredicate<ConstructorPredicate, ConstructorAccessor, Constructor<?>> {

  public ConstructorPredicate(
      @NotNull Function<ConstructorPredicate, Stream<ConstructorAccessor>> producer,
      @NotNull Supplier<String> className) {
    super(producer, () -> String.format("Can't find constructor in class %s matching: ", className.get()));
  }

  @Override
  protected @NotNull ConstructorPredicate instance() {
    return this;
  }
}
