package dev.imprex.orebfuscator.reflect.predicate;

import dev.imprex.orebfuscator.reflect.accessor.MethodAccessor;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class MethodPredicate extends AbstractExecutablePredicate<MethodPredicate, MethodAccessor, Method> {

  private @Nullable ClassPredicate returnType;

  public MethodPredicate(
      Function<MethodPredicate, Stream<MethodAccessor>> producer,
      Supplier<String> className) {
    super(producer, () -> String.format("Can't find constructor in class %s matching: ", className.get()));
  }

  @Override
  public boolean test(Method method) {
    return super.test(method)
        && (returnType == null || returnType.test(method.getReturnType()));
  }

  @Override
  void requirements(RequirementCollector collector) {
    super.requirements(collector);

    if (returnType != null) {
      collector.collect("returnType", returnType.requirement());
    }
  }

  public MethodPredicate returnType(ClassPredicate matcher) {
    this.returnType = Objects.requireNonNull(matcher);
    return this;
  }

  public ClassPredicate.Builder<MethodPredicate> returnType() {
    return new ClassPredicate.Builder<>(this::returnType);
  }

  @Override
  protected MethodPredicate instance() {
    return this;
  }
}
