package dev.imprex.orebfuscator.reflect.predicate;

import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;

@NullMarked
public interface ClassPredicate extends Predicate<Class<?>> {

  boolean test(Class<?> type);

  String requirement();

  record Builder<TParent>(Function<ClassPredicate, TParent> returnFunction) {

    public TParent is(Class<?> type) {
      return returnFunction.apply(new IsClassPredicate(type));
    }

    public TParent superOf(Class<?> type) {
      return returnFunction.apply(new SuperClassPredicate(type));
    }

    public TParent subOf(Class<?> type) {
      return returnFunction.apply(new SubClassPredicate(type));
    }

    public TParent any(Set<Class<?>> types) {
      return returnFunction.apply(new AnyClassPredicate(types));
    }

    public TParent any(Class<?>... types) {
      return any(Set.of(types));
    }

    public TParent regex(Pattern pattern) {
      return returnFunction.apply(new RegexClassPredicate(pattern));
    }
  }

  record IsClassPredicate(Class<?> expected) implements ClassPredicate {

    public IsClassPredicate {
      Objects.requireNonNull(expected);
    }

    @Override
    public boolean test(Class<?> type) {
      Objects.requireNonNull(type);

      return this.expected.equals(type);
    }

    @Override
    public String requirement() {
      return String.format("{is %s}", this.expected.getTypeName());
    }

    @Override
    public boolean equals(Object obj) {
      return this == obj || (obj instanceof IsClassPredicate other && Objects.equals(this.expected, other.expected));
    }
  }

  record SuperClassPredicate(Class<?> expected) implements ClassPredicate {

    public SuperClassPredicate {
      Objects.requireNonNull(expected);
    }

    @Override
    public boolean test(Class<?> type) {
      Objects.requireNonNull(type);

      return type.isAssignableFrom(this.expected);
    }

    @Override
    public String requirement() {
      return String.format("{super-class-of %s}", this.expected.getTypeName());
    }

    @Override
    public boolean equals(Object obj) {
      return this == obj || (obj instanceof SuperClassPredicate other && Objects.equals(this.expected, other.expected));
    }
  }

  record SubClassPredicate(Class<?> expected) implements ClassPredicate {

    public SubClassPredicate {
      Objects.requireNonNull(expected);
    }

    @Override
    public boolean test(Class<?> type) {
      Objects.requireNonNull(type);

      return this.expected.isAssignableFrom(type);
    }

    @Override
    public String requirement() {
      return String.format("{sub-class-of %s}", this.expected.getTypeName());
    }

    @Override
    public boolean equals(Object obj) {
      return this == obj || (obj instanceof SubClassPredicate other && Objects.equals(this.expected, other.expected));
    }
  }

  record AnyClassPredicate(Set<Class<?>> expected) implements ClassPredicate {

    public AnyClassPredicate {
      Objects.requireNonNull(expected);
    }

    @Override
    public boolean test(Class<?> type) {
      Objects.requireNonNull(type);

      return this.expected.contains(type);
    }

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

  record RegexClassPredicate(Pattern expected) implements ClassPredicate {

    public RegexClassPredicate {
      Objects.requireNonNull(expected);
    }

    @Override
    public boolean test(Class<?> type) {
      Objects.requireNonNull(type);

      return this.expected.matcher(type.getTypeName()).matches();
    }

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
