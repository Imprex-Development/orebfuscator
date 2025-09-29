package dev.imprex.orebfuscator.reflect.predicate;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.imprex.orebfuscator.reflect.accessor.MemberAccessor;

abstract sealed class AbstractExecutablePredicate<
    TThis extends AbstractExecutablePredicate<TThis, TAccessor, TExecutable>,
    TAccessor extends MemberAccessor<TExecutable>,
    TExecutable extends Executable
    > extends AbstractMemberPredicate<TThis, TAccessor, TExecutable> permits ConstructorPredicate, MethodPredicate {

  private final @NotNull List<IndexedClassMatcher> exceptionClass = new ArrayList<>();
  private final @NotNull List<IndexedClassMatcher> parameterClass = new ArrayList<>();
  private int parameterCount = -1;

  public AbstractExecutablePredicate(
      @NotNull Function<TThis, Stream<TAccessor>> producer,
      @NotNull Supplier<String> error) {
    super(producer, error);
  }

  @Override
  public boolean test(@NotNull TExecutable executable) {
    return super.test(executable)
        && IndexedClassMatcher.all(executable.getExceptionTypes(), exceptionClass)
        && IndexedClassMatcher.all(executable.getParameterTypes(), parameterClass)
        && (parameterCount < 0 || parameterCount == executable.getParameterCount());
  }

  @Override
  void requirements(@NotNull RequirementCollector collector) {
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

  public @NotNull TThis exception(@NotNull ClassPredicate matcher) {
    this.exceptionClass.add(new IndexedClassMatcher(Objects.requireNonNull(matcher)));
    return instance();
  }

  public @NotNull TThis exception(@NotNull ClassPredicate matcher, int index) {
    this.exceptionClass.add(new IndexedClassMatcher(Objects.requireNonNull(matcher), index));
    return instance();
  }

  public @NotNull ClassPredicate.Builder<TThis> exception() {
    return new ClassPredicate.Builder<>(this::exception);
  }

  public @NotNull ClassPredicate.Builder<TThis> exception(int index) {
    return new ClassPredicate.Builder<>(m -> this.exception(m, index));
  }

  public @NotNull TThis parameter(@NotNull ClassPredicate matcher) {
    this.parameterClass.add(new IndexedClassMatcher(Objects.requireNonNull(matcher)));
    return instance();
  }

  public @NotNull TThis parameter(@NotNull ClassPredicate matcher, int index) {
    this.parameterClass.add(new IndexedClassMatcher(Objects.requireNonNull(matcher), index));
    return instance();
  }

  public @NotNull ClassPredicate.Builder<TThis> parameter() {
    return new ClassPredicate.Builder<>(this::parameter);
  }

  public @NotNull ClassPredicate.Builder<TThis> parameter(int index) {
    return new ClassPredicate.Builder<>(m -> this.parameter(m, index));
  }

  public @NotNull TThis parameterCount(int parameterCount) {
    this.parameterCount = parameterCount;
    return instance();
  }

  private record IndexedClassMatcher(@NotNull ClassPredicate matcher, @Nullable Integer index) implements
      Comparable<IndexedClassMatcher> {

    private static boolean all(@NotNull Class<?>[] classArray, @NotNull List<IndexedClassMatcher> classMatchers) {
      return classMatchers.stream().allMatch(matcher -> matcher.matches(classArray));
    }

    private static String toString(@NotNull List<IndexedClassMatcher> classMatchers) {
      return classMatchers.stream()
          .sorted()
          .map(IndexedClassMatcher::toString)
          .collect(Collectors.joining(",\n    ", "{\n    ", "\n  }"));
    }

    public IndexedClassMatcher(@NotNull ClassPredicate matcher) {
      this(matcher, null);
    }

    public boolean matches(@NotNull Class<?>[] classArray) {
      if (index() == null) {
        for (Class<?> entry : classArray) {
          if (matcher().test(entry)) {
            return true;
          }
        }
        return false;
      }

      return index() < classArray.length && matcher().test(classArray[index()]);
    }

    @Override
    public int compareTo(@NotNull IndexedClassMatcher other) {
      if (this.index == null && other.index == null) {
        return 0;
      }
      if (this.index == null) {
        return -1;
      }
      if (other.index == null) {
        return 1;
      }
      return this.index.compareTo(other.index);
    }

    @Override
    public @NotNull String toString() {
      String key = index() == null ? "<any>" : index().toString();
      return String.format("%s=%s", key, matcher().requirement());
    }
  }
}
