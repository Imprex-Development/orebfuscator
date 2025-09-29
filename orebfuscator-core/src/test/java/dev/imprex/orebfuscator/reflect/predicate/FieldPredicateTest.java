package dev.imprex.orebfuscator.reflect.predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import dev.imprex.orebfuscator.reflect.Reflector;
import dev.imprex.orebfuscator.reflect.dummy.Entity;

class FieldPredicateTest {

  @Test
  void testType() {
    Reflector reflector = Reflector.of(Entity.class);

    assertEquals(3, reflector.field()
        .type().is(int.class)
        .stream().count());
    assertEquals(2, reflector.field()
        .type().is(String.class)
        .stream().count());
  }
}
