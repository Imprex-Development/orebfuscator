package dev.imprex.orebfuscator.reflect.predicate;

import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import dev.imprex.orebfuscator.reflect.accessor.MemberAccessor;

abstract sealed class AbstractMemberPredicate<
    TThis extends AbstractMemberPredicate<TThis, TAccessor, TMember>,
    TAccessor extends MemberAccessor<TMember>,
    TMember extends Member
    > implements Predicate<TMember> permits AbstractExecutablePredicate, FieldPredicate {

  private final @NotNull Function<TThis, Stream<TAccessor>> producer;
  private final @NotNull Supplier<String> error;

  private int requiredModifiers;
  private int bannedModifiers;
  private boolean includeSynthetic;
  private @Nullable Pattern name;
  private @Nullable ClassPredicate declaringClass;

  public AbstractMemberPredicate(
      @NotNull Function<TThis, Stream<TAccessor>> producer,
      @NotNull Supplier<String> error) {
    this.producer = producer;
    this.error = error;
  }

  @Override
  public boolean test(@NotNull TMember member) {
    int modifiers = member.getModifiers();
    return (modifiers & requiredModifiers) == requiredModifiers
        && (modifiers & bannedModifiers) == 0
        && (includeSynthetic || !member.isSynthetic())
        && (name == null || name.matcher(member.getName()).matches())
        && (declaringClass == null || declaringClass.test(member.getDeclaringClass()));
  }

  void requirements(@NotNull RequirementCollector collector) {
    if (requiredModifiers != 0) {
      collector.collect("requiredModifiers", Modifier.toString(requiredModifiers));
    }
    if (bannedModifiers != 0) {
      collector.collect("bannedModifiers", Modifier.toString(bannedModifiers));
    }
    if (includeSynthetic) {
      collector.collect("includeSynthetic");
    }
    if (name != null) {
      collector.collect("name", name);
    }
    if (declaringClass != null) {
      collector.collect("declaringClass", declaringClass.requirement());
    }
  }

  private @NotNull IllegalArgumentException requirementException() {
    var collector = new RequirementCollector(error.get());
    requirements(collector);
    return new IllegalArgumentException(collector.get());
  }

  protected abstract @NotNull TThis instance();

  public @NotNull TThis requireModifier(@MagicConstant(flagsFromClass = Modifier.class) int modifiers) {
    this.requiredModifiers |= modifiers;
    return instance();
  }

  public @NotNull TThis requirePublic() {
    return requireModifier(Modifier.PUBLIC);
  }

  public @NotNull TThis requireProtected() {
    return requireModifier(Modifier.PROTECTED);
  }

  public @NotNull TThis requirePrivate() {
    return requireModifier(Modifier.PRIVATE);
  }

  public @NotNull TThis requireStatic() {
    return requireModifier(Modifier.STATIC);
  }

  public @NotNull TThis requireFinal() {
    return requireModifier(Modifier.FINAL);
  }

  public @NotNull TThis banModifier(@MagicConstant(flagsFromClass = Modifier.class) int modifiers) {
    this.bannedModifiers |= modifiers;
    return instance();
  }

  public @NotNull TThis banPublic() {
    return banModifier(Modifier.PUBLIC);
  }

  public @NotNull TThis banProtected() {
    return banModifier(Modifier.PROTECTED);
  }

  public @NotNull TThis banPrivate() {
    return banModifier(Modifier.PRIVATE);
  }

  public @NotNull TThis banStatic() {
    return banModifier(Modifier.STATIC);
  }

  public @NotNull TThis banFinal() {
    return banModifier(Modifier.FINAL);
  }

  public @NotNull TThis includeSynthetic() {
    this.includeSynthetic = true;
    return instance();
  }

  public @NotNull TThis nameRegex(@NotNull Pattern pattern) {
    this.name = Objects.requireNonNull(pattern);
    return instance();
  }

  public @NotNull TThis nameIs(@NotNull String name) {
    String pattern = Pattern.quote(Objects.requireNonNull(name));
    return nameRegex(Pattern.compile(pattern));
  }

  public @NotNull TThis nameIsIgnoreCase(@NotNull String name) {
    String pattern = Pattern.quote(Objects.requireNonNull(name));
    return nameRegex(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
  }

  public @NotNull TThis declaringClass(@NotNull ClassPredicate matcher) {
    this.declaringClass = Objects.requireNonNull(matcher);
    return instance();
  }

  public @NotNull ClassPredicate.Builder<TThis> declaringClass() {
    return new ClassPredicate.Builder<>(this::declaringClass);
  }

  @Contract(pure = true)
  public @NotNull Stream<TAccessor> stream() {
    return producer.apply(instance());
  }

  @Contract(pure = true)
  public @Nullable TAccessor get(int index) {
    return stream().skip(index).findFirst().orElse(null);
  }

  @Contract(pure = true)
  public @NotNull TAccessor getOrThrow(int index) {
    return stream().skip(index).findFirst().orElseThrow(this::requirementException);
  }

  @Contract(pure = true)
  public @Nullable TAccessor first() {
    return stream().findFirst().orElse(null);
  }

  @Contract(pure = true)
  public @NotNull TAccessor firstOrThrow() {
    return stream().findFirst().orElseThrow(this::requirementException);
  }

  @Contract(pure = true)
  public @Nullable TAccessor find(@NotNull Predicate<TMember> predicate) {
    return stream().filter(accessor -> predicate.test(accessor.member()))
        .findFirst().orElse(null);
  }

  @Contract(pure = true)
  public @NotNull TAccessor findOrThrow(@NotNull Predicate<TMember> predicate) {
    return stream().filter(accessor -> predicate.test(accessor.member()))
        .findFirst().orElseThrow(this::requirementException);
  }
}
