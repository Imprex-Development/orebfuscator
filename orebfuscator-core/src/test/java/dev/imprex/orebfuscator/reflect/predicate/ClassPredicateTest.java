package dev.imprex.orebfuscator.reflect.predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import dev.imprex.orebfuscator.reflect.dummy.Entity;
import dev.imprex.orebfuscator.reflect.dummy.Id;
import dev.imprex.orebfuscator.reflect.dummy.Player;

class ClassPredicateTest {

  @Test
  void testIsClassPredicate() {
    ClassPredicate predicate = new ClassPredicate.IsClassPredicate(Entity.class);

    assertFalse(predicate.test(Id.class));
    assertTrue(predicate.test(Entity.class));
    assertFalse(predicate.test(Player.class));

    assertThrows(NullPointerException.class, () -> new ClassPredicate.IsClassPredicate(null));
    assertThrows(NullPointerException.class, () -> predicate.test(null));

    assertEquals("{is dev.imprex.orebfuscator.reflect.dummy.Entity}", predicate.requirement());
  }

  @Test
  void testSuperClassPredicate() {
    ClassPredicate predicate = new ClassPredicate.SuperClassPredicate(Entity.class);

    assertTrue(predicate.test(Id.class));
    assertTrue(predicate.test(Entity.class));
    assertFalse(predicate.test(Player.class));

    assertThrows(NullPointerException.class, () -> new ClassPredicate.SuperClassPredicate(null));
    assertThrows(NullPointerException.class, () -> predicate.test(null));

    assertEquals("{super-class-of dev.imprex.orebfuscator.reflect.dummy.Entity}", predicate.requirement());
  }

  @Test
  void testSubClassPredicate() {
    ClassPredicate predicate = new ClassPredicate.SubClassPredicate(Entity.class);

    assertFalse(predicate.test(Id.class));
    assertTrue(predicate.test(Entity.class));
    assertTrue(predicate.test(Player.class));

    assertThrows(NullPointerException.class, () -> new ClassPredicate.SubClassPredicate(null));
    assertThrows(NullPointerException.class, () -> predicate.test(null));

    assertEquals("{sub-class-of dev.imprex.orebfuscator.reflect.dummy.Entity}", predicate.requirement());
  }

  @Test
  void testAnyClassPredicate() {
    // we use a LinkedHashSet here to preserve iteration order for the requirement string
    Set<Class<?>> set = new LinkedHashSet<>(List.of(Id.class, Entity.class));
    ClassPredicate predicate = new ClassPredicate.AnyClassPredicate(set);

    assertTrue(predicate.test(Id.class));
    assertTrue(predicate.test(Entity.class));
    assertFalse(predicate.test(Player.class));

    assertThrows(NullPointerException.class, () -> new ClassPredicate.AnyClassPredicate(null));
    assertThrows(NullPointerException.class, () -> predicate.test(null));

    assertEquals("{any (dev.imprex.orebfuscator.reflect.dummy.Id, dev.imprex.orebfuscator.reflect.dummy.Entity)}",
        predicate.requirement());
  }

  @Test
  void testRegexClassPredicate() {
    Pattern pattern = Pattern.compile(".*y", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    ClassPredicate predicate = new ClassPredicate.RegexClassPredicate(pattern);

    assertFalse(predicate.test(Id.class));
    assertTrue(predicate.test(Entity.class));
    assertFalse(predicate.test(Player.class));

    assertThrows(NullPointerException.class, () -> new ClassPredicate.RegexClassPredicate(null));
    assertThrows(NullPointerException.class, () -> predicate.test(null));

    assertEquals("{regex .*y}", predicate.requirement());
  }

  @Test
  void testBuilder() {
    ClassPredicate.Builder<ClassPredicate> builder = new ClassPredicate.Builder<>(Function.identity());

    assertEquals(new ClassPredicate.IsClassPredicate(Entity.class), builder.is(Entity.class));
    assertEquals(new ClassPredicate.SuperClassPredicate(Entity.class), builder.superOf(Entity.class));
    assertEquals(new ClassPredicate.SubClassPredicate(Entity.class), builder.subOf(Entity.class));

    ClassPredicate expected = new ClassPredicate.AnyClassPredicate(Set.of(Id.class, Entity.class));
    assertEquals(expected, builder.any(Id.class, Entity.class));
    assertEquals(expected, builder.any(Set.of(Id.class, Entity.class)));

    Pattern pattern = Pattern.compile("foo");
    assertEquals(new ClassPredicate.RegexClassPredicate(pattern), builder.regex(pattern));
  }
}
