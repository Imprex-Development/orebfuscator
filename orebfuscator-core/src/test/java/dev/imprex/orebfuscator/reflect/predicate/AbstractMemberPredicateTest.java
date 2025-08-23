package dev.imprex.orebfuscator.reflect.predicate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Modifier;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import dev.imprex.orebfuscator.reflect.Reflector;
import dev.imprex.orebfuscator.reflect.dummy.Entity;
import dev.imprex.orebfuscator.reflect.dummy.Player;
import dev.imprex.orebfuscator.reflect.predicate.ClassPredicate.IsClassPredicate;

class AbstractMemberPredicateTest {

  @Test
  void testRequireModifier() throws Exception {
    Reflector reflector = Reflector.of(Entity.class);

    assertEquals(1, reflector.field()
        .requireModifier(Modifier.VOLATILE)
        .stream().count());
    assertEquals(1, reflector.field()
        .requirePublic()
        .stream().count());
    assertEquals(1, reflector.field()
        .requireProtected()
        .stream().count());
    assertEquals(3, reflector.field()
        .requirePrivate()
        .stream().count());
    assertEquals(2, reflector.field()
        .requireStatic()
        .stream().count());
    assertEquals(2, reflector.field()
        .requireFinal()
        .stream().count());
  }

  @Test
  void testBanModifier() throws Exception {
    Reflector reflector = Reflector.of(Entity.class);

    assertEquals(4, reflector.field()
        .banModifier(Modifier.VOLATILE)
        .stream().count());
    assertEquals(4, reflector.field()
        .banPublic()
        .stream().count());
    assertEquals(4, reflector.field()
        .banProtected()
        .stream().count());
    assertEquals(2, reflector.field()
        .banPrivate()
        .stream().count());
    assertEquals(3, reflector.field()
        .banStatic()
        .stream().count());
    assertEquals(3, reflector.field()
        .banFinal()
        .stream().count());
  }

  @Test
  void testName() throws Exception {
    Reflector reflector = Reflector.of(Entity.class);

    assertEquals(2, reflector.field()
        .nameRegex(Pattern.compile(".*id", Pattern.CASE_INSENSITIVE | Pattern.DOTALL))
        .stream().count());
    assertEquals(1, reflector.field()
        .nameIs("entityId")
        .stream().count());
    assertEquals(1, reflector.field()
        .nameIsIgnoreCase("entityid")
        .stream().count());
  }

  @Test
  void testDeclaringClass() throws Exception {
    Reflector reflector = Reflector.of(Entity.class);

    assertEquals(5, reflector.field()
        .declaringClass().is(Entity.class)
        .stream().count());
    assertEquals(5, reflector.field()
        .declaringClass(new IsClassPredicate(Entity.class))
        .stream().count());

    assertEquals(0, reflector.field()
        .declaringClass().is(Player.class)
        .stream().count());
    assertEquals(0, reflector.field()
        .declaringClass(new IsClassPredicate(Player.class))
        .stream().count());
  }

  @Test
  void testAll() throws Exception {
    Reflector reflector = Reflector.of(Entity.class);

    assertEquals(1, reflector.constructor().stream().count());
    assertEquals(5, reflector.field().stream().count());
    assertEquals(7, reflector.method().stream().count());
  }

  @Test
  void testGet() throws Exception {
    Reflector reflector = Reflector.of(Entity.class);

    assertNotNull(reflector.constructor().get(0));
    assertNull(reflector.constructor().get(1));
  }

  @Test
  void testGetOrThrow() throws Exception {
    Reflector reflector = Reflector.of(Entity.class);

    assertDoesNotThrow(() -> reflector.constructor().getOrThrow(0));
    assertThrows(IllegalArgumentException.class, () -> reflector.constructor().getOrThrow(1));
  }

  @Test
  void testFirst() throws Exception {
    Reflector reflector = Reflector.of(Entity.class);

    assertNotNull(reflector.constructor().first());
    assertNull(reflector.constructor().parameterCount(0).first());
  }

  @Test
  void testFirstOrThrow() throws Exception {
    Reflector reflector = Reflector.of(Entity.class);

    assertDoesNotThrow(() -> reflector.constructor().firstOrThrow());
    assertThrows(IllegalArgumentException.class, () -> reflector.constructor().parameterCount(0).firstOrThrow());
  }

  @Test
  void testFind() throws Exception {
    Reflector reflector = Reflector.of(Entity.class);

    assertNotNull(reflector.constructor().find(c -> c.getParameterCount() != 0));
    assertNull(reflector.constructor().find(c -> c.getParameterCount() == 0));
  }

  @Test
  void testFindOrThrow() throws Exception {
    Reflector reflector = Reflector.of(Entity.class);

    assertDoesNotThrow(() -> reflector.constructor().findOrThrow(c -> c.getParameterCount() != 0));
    assertThrows(IllegalArgumentException.class,
        () -> reflector.constructor().findOrThrow(c -> c.getParameterCount() == 0));
  }
}
