package dev.imprex.orebfuscator.reflect.predicate;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

public interface ClassPredicate extends Predicate<Class<?>> {

  boolean test(@NotNull Class<?> type);

  @NotNull
  String requirement();

  class Builder<TParent> {

    private final Function<ClassPredicate, TParent> returnFunction;

    public Builder(Function<ClassPredicate, TParent> returnFunction) {
      this.returnFunction = returnFunction;
    }

    @NotNull
    public TParent is(@NotNull Class<?> type) {
      return returnFunction.apply(new IsClassPredicate(type));
    }

    @NotNull
    public TParent superOf(@NotNull Class<?> type) {
      return returnFunction.apply(new SuperClassPredicate(type));
    }

    @NotNull
    public TParent subOf(@NotNull Class<?> type) {
      return returnFunction.apply(new SubClassPredicate(type));
    }

    @NotNull
    public TParent any(@NotNull Set<Class<?>> types) {
      return returnFunction.apply(new AnyClassPredicate(types));
    }

    @NotNull
    public TParent any(@NotNull Class<?>... types) {
      return any(Set.of(types));
    }

    @NotNull
    public TParent regex(@NotNull Pattern pattern) {
      return returnFunction.apply(new RegexClassPredicate(pattern));
    }
  }

  class IsClassPredicate implements ClassPredicate {

    private final @NotNull Class<?> expected;

    public IsClassPredicate(@NotNull Class<?> expected) {
      this.expected = Objects.requireNonNull(expected);
    }

    @Override
    public boolean test(@NotNull Class<?> type) {
      Objects.requireNonNull(type);

      return this.expected.equals(type);
    }

    @NotNull
    @Override
    public String requirement() {
      return String.format("{is %s}", this.expected.getTypeName());
    }

    @Override
    public boolean equals(Object obj) {
      return this == obj || (obj instanceof IsClassPredicate other && Objects.equals(this.expected, other.expected));
    }
  }

  class SuperClassPredicate implements ClassPredicate {

    private final @NotNull Class<?> expected;

    public SuperClassPredicate(@NotNull Class<?> expected) {
      this.expected = Objects.requireNonNull(expected);
    }

    @Override
    public boolean test(@NotNull Class<?> type) {
      Objects.requireNonNull(type);

      return type.isAssignableFrom(this.expected);
    }

    @NotNull
    @Override
    public String requirement() {
      return String.format("{super-class-of %s}", this.expected.getTypeName());
    }

    @Override
    public boolean equals(Object obj) {
      return this == obj || (obj instanceof SuperClassPredicate other && Objects.equals(this.expected, other.expected));
    }
  }

  class SubClassPredicate implements ClassPredicate {

    private final @NotNull Class<?> expected;

    public SubClassPredicate(@NotNull Class<?> expected) {
      this.expected = Objects.requireNonNull(expected);
    }

    @Override
    public boolean test(@NotNull Class<?> type) {
      Objects.requireNonNull(type);

      return this.expected.isAssignableFrom(type);
    }

    @NotNull
    @Override
    public String requirement() {
      return String.format("{sub-class-of %s}", this.expected.getTypeName());
    }

    @Override
    public boolean equals(Object obj) {
      return this == obj || (obj instanceof SubClassPredicate other && Objects.equals(this.expected, other.expected));
    }
  }

  class AnyClassPredicate implements ClassPredicate {

    private final @NotNull Set<Class<?>> expected;

    public AnyClassPredicate(@NotNull Set<Class<?>> expected) {
      this.expected = Objects.requireNonNull(expected);
    }

    @Override
    public boolean test(@NotNull Class<?> type) {
      Objects.requireNonNull(type);

      return this.expected.contains(type);
    }

    @NotNull
    @Override
    public String requirement() {
      return String.format("{any %s}",
          this.expected.stream().map(Class::getTypeName).collect(Collectors.joining(", ", "(", ")")));
    }

    @Override
    public boolean equals(Object obj) {
      return this == obj || (obj instanceof AnyClassPredicate other && Objects.equals(this.expected, other.expected));
    }
  }

  class RegexClassPredicate implements ClassPredicate {

    private final @NotNull Pattern expected;

    public RegexClassPredicate(@NotNull Pattern expected) {
      this.expected = Objects.requireNonNull(expected);
    }

    @Override
    public boolean test(@NotNull Class<?> type) {
      Objects.requireNonNull(type);

      return this.expected.matcher(type.getTypeName()).matches();
    }

    @NotNull
    @Override
    public String requirement() {
      return String.format("{regex %s}", this.expected);
    }

    @Override
    public boolean equals(Object obj) {
      return this == obj || (obj instanceof RegexClassPredicate other && Objects.equals(this.expected, other.expected));
    }
  }
}
