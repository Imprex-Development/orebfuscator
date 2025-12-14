package dev.imprex.orebfuscator.reflect.predicate;

import java.lang.reflect.Constructor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;


import dev.imprex.orebfuscator.reflect.accessor.ConstructorAccessor;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ConstructorPredicate
    extends AbstractExecutablePredicate<ConstructorPredicate, ConstructorAccessor, Constructor<?>> {

  public ConstructorPredicate(
      Function<ConstructorPredicate, Stream<ConstructorAccessor>> producer,
      Supplier<String> className) {
    super(producer, () -> String.format("Can't find constructor in class %s matching: ", className.get()));
  }

  @Override
  protected ConstructorPredicate instance() {
    return this;
  }
}
