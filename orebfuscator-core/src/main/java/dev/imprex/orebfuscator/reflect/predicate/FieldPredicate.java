package dev.imprex.orebfuscator.reflect.predicate;

import dev.imprex.orebfuscator.reflect.accessor.FieldAccessor;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class FieldPredicate extends AbstractMemberPredicate<FieldPredicate, FieldAccessor, Field> {

  private @Nullable ClassPredicate type;

  public FieldPredicate(
      @NotNull Function<FieldPredicate, Stream<FieldAccessor>> producer,
      @NotNull Supplier<String> className) {
    super(producer, () -> String.format("Can't find field in class %s matching: ", className.get()));
  }

  @Override
  public boolean test(@NotNull Field field) {
    return super.test(field)
        && (type == null || type.test(field.getType()));
  }

  @Override
  void requirements(@NotNull RequirementCollector collector) {
    super.requirements(collector);

    if (type != null) {
      collector.collect("type", type.requirement());
    }
  }

  public @NotNull FieldPredicate type(@NotNull ClassPredicate matcher) {
    this.type = Objects.requireNonNull(matcher);
    return this;
  }

  public @NotNull ClassPredicate.Builder<FieldPredicate> type() {
    return new ClassPredicate.Builder<>(this::type);
  }

  @Override
  protected @NotNull FieldPredicate instance() {
    return this;
  }
}
