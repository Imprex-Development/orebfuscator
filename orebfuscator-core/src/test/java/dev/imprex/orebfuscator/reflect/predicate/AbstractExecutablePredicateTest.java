package dev.imprex.orebfuscator.reflect.predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import dev.imprex.orebfuscator.reflect.Reflector;
import dev.imprex.orebfuscator.reflect.dummy.Entity;

class AbstractExecutablePredicateTest {

  @Test
  void testException() {
    Reflector reflector = Reflector.of(Entity.class);

    assertEquals(1, reflector.method()
        .exception(0).is(NullPointerException.class)
        .stream().count());
    assertEquals(1, reflector.method()
        .exception(1).is(NullPointerException.class)
        .stream().count());
    assertEquals(0, reflector.method()
        .exception(2).is(NullPointerException.class)
        .stream().count());

    assertEquals(2, reflector.method()
        .exception().is(NullPointerException.class)
        .stream().count());
    assertEquals(1, reflector.method()
        .exception().is(NullPointerException.class)
        .exception().is(IOException.class)
        .stream().count());
  }

  @Test
  void testParameter() {
    Reflector reflector = Reflector.of(Entity.class);

    assertEquals(1, reflector.method()
        .parameter(0).is(int.class)
        .stream().count());
    assertEquals(1, reflector.method()
        .parameter(1).is(int.class)
        .stream().count());
    assertEquals(0, reflector.method()
        .parameter(2).is(int.class)
        .stream().count());

    assertEquals(2, reflector.method()
        .parameter().is(int.class)
        .stream().count());
    assertEquals(1, reflector.method()
        .parameter().is(int.class)
        .parameter().is(String.class)
        .stream().count());
  }

  @Test
  void testParameterCount() {
    Reflector reflector = Reflector.of(Entity.class);

    assertEquals(5, reflector.method()
        .parameterCount(0)
        .stream().count());
    assertEquals(1, reflector.method()
        .parameterCount(1)
        .stream().count());
    assertEquals(1, reflector.method()
        .parameterCount(2)
        .stream().count());
    assertEquals(0, reflector.method()
        .parameterCount(3)
        .stream().count());
  }
}
