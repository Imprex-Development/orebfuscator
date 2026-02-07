package dev.imprex.orebfuscator.reflect.predicate;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import dev.imprex.orebfuscator.reflect.accessor.MemberAccessor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
abstract sealed class AbstractExecutablePredicate<
    TThis extends AbstractExecutablePredicate<TThis, TAccessor, TExecutable>,
    TAccessor extends MemberAccessor<TExecutable>,
    TExecutable extends Executable
    > extends AbstractMemberPredicate<TThis, TAccessor, TExecutable> permits ConstructorPredicate, MethodPredicate {

  private final List<IndexedClassMatcher> exceptionClass = new ArrayList<>();
  private final List<IndexedClassMatcher> parameterClass = new ArrayList<>();
  private int parameterCount = -1;

  public AbstractExecutablePredicate(
      Function<TThis, Stream<TAccessor>> producer,
      Supplier<String> error) {
    super(producer, error);
  }

  @Override
  public boolean test(TExecutable executable) {
    return super.test(executable)
        && IndexedClassMatcher.all(executable.getExceptionTypes(), exceptionClass)
        && IndexedClassMatcher.all(executable.getParameterTypes(), parameterClass)
        && (parameterCount < 0 || parameterCount == executable.getParameterCount());
  }

  @Override
  void requirements(RequirementCollector collector) {
    super.requirements(collector);

    if (!exceptionClass.isEmpty()) {
      collector.collect("exceptionClass", IndexedClassMatcher.toString(exceptionClass));
    }
    if (!parameterClass.isEmpty()) {
      collector.collect("parameterClass", IndexedClassMatcher.toString(parameterClass));
    }
    if (parameterCount >= 0) {
      collector.collect("parameterCount", parameterCount);
    }
  }

  public TThis exception(ClassPredicate matcher) {
    this.exceptionClass.add(new IndexedClassMatcher(Objects.requireNonNull(matcher)));
    return instance();
  }

  public TThis exception(ClassPredicate matcher, int index) {
    this.exceptionClass.add(new IndexedClassMatcher(Objects.requireNonNull(matcher), index));
    return instance();
  }

  public ClassPredicate.Builder<TThis> exception() {
    return new ClassPredicate.Builder<>(this::exception);
  }

  public ClassPredicate.Builder<TThis> exception(int index) {
    return new ClassPredicate.Builder<>(m -> this.exception(m, index));
  }

  public TThis parameter(ClassPredicate matcher) {
    this.parameterClass.add(new IndexedClassMatcher(Objects.requireNonNull(matcher)));
    return instance();
  }

  public TThis parameter(ClassPredicate matcher, int index) {
    this.parameterClass.add(new IndexedClassMatcher(Objects.requireNonNull(matcher), index));
    return instance();
  }

  public ClassPredicate.Builder<TThis> parameter() {
    return new ClassPredicate.Builder<>(this::parameter);
  }

  public ClassPredicate.Builder<TThis> parameter(int index) {
    return new ClassPredicate.Builder<>(m -> this.parameter(m, index));
  }

  public TThis parameterCount(int parameterCount) {
    this.parameterCount = parameterCount;
    return instance();
  }

  private record IndexedClassMatcher(ClassPredicate matcher, OptionalInt index) implements
      Comparable<IndexedClassMatcher> {

    private static boolean all(Class<?>[] classArray, List<IndexedClassMatcher> classMatchers) {
      return classMatchers.stream().allMatch(matcher -> matcher.matches(classArray));
    }

    private static String toString(List<IndexedClassMatcher> classMatchers) {
      return classMatchers.stream().sorted().map(IndexedClassMatcher::toString)
          .collect(Collectors.joining(",\n    ", "{\n    ", "\n  }"));
    }

    public IndexedClassMatcher(ClassPredicate matcher) {
      this(matcher, OptionalInt.empty());
    }

    public IndexedClassMatcher(ClassPredicate matcher, int index) {
      this(matcher, OptionalInt.of(index));
    }

    public boolean matches(Class<?>[] classArray) {
      if (index.isEmpty()) {
        for (Class<?> entry : classArray) {
          if (matcher.test(entry)) {
            return true;
          }
        }
        return false;
      }

      int i = index.getAsInt();
      return i < classArray.length && matcher.test(classArray[i]);
    }

    @Override
    public int compareTo(IndexedClassMatcher other) {
      if (this.index.isEmpty() && other.index.isEmpty()) {
        return 0;
      }
      if (this.index.isEmpty()) {
        return -1;
      }
      if (other.index.isEmpty()) {
        return 1;
      }
      return Integer.compare(this.index.getAsInt(), other.index.getAsInt());
    }

    @Override
    public String toString() {
      String key = index.isEmpty() ? "<any>" : Integer.toString(index.getAsInt());
      return String.format("%s=%s", key, matcher.requirement());
    }
  }
}
