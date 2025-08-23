package dev.imprex.orebfuscator.reflect.predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import dev.imprex.orebfuscator.reflect.Reflector;
import dev.imprex.orebfuscator.reflect.dummy.Entity;

class MethodPredicateTest {

  @Test
  void testReturnType() {
    Reflector reflector = Reflector.of(Entity.class);

    assertEquals(2, reflector.method()
        .returnType().is(void.class)
        .stream().count());
    assertEquals(3, reflector.method()
        .returnType().is(int.class)
        .stream().count());
    assertEquals(1, reflector.method()
        .returnType().is(String.class)
        .stream().count());
  }
}
