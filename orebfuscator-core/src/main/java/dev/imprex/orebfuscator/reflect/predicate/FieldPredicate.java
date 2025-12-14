package dev.imprex.orebfuscator.reflect.predicate;

import dev.imprex.orebfuscator.reflect.accessor.FieldAccessor;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class FieldPredicate extends AbstractMemberPredicate<FieldPredicate, FieldAccessor, Field> {

  private @Nullable ClassPredicate type;

  public FieldPredicate(
      Function<FieldPredicate, Stream<FieldAccessor>> producer,
      Supplier<String> className) {
    super(producer, () -> String.format("Can't find field in class %s matching: ", className.get()));
  }

  @Override
  public boolean test(Field field) {
    return super.test(field)
        && (type == null || type.test(field.getType()));
  }

  @Override
  void requirements(RequirementCollector collector) {
    super.requirements(collector);

    if (type != null) {
      collector.collect("type", type.requirement());
    }
  }

  public FieldPredicate type(ClassPredicate matcher) {
    this.type = Objects.requireNonNull(matcher);
    return this;
  }

  public ClassPredicate.Builder<FieldPredicate> type() {
    return new ClassPredicate.Builder<>(this::type);
  }

  @Override
  protected FieldPredicate instance() {
    return this;
  }
}
