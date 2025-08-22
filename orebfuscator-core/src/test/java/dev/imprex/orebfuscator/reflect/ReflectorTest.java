package dev.imprex.orebfuscator.reflect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import dev.imprex.orebfuscator.reflect.dummy.Entity;
import dev.imprex.orebfuscator.reflect.dummy.Player;

class ReflectorTest {

  private static final String REQUIREMENTS_FIELD = decode(
      "ewogIHJlcXVpcmVkTW9kaWZpZXJzOiBwcml2YXRlIHN5bmNocm9uaXplZCwKICBiYW5uZWRNb2RpZmllcnM6IHN0YXRpYywKICBpbmNsdWRlU3ludGhldGljLAogIG5hbWU6IFxRdGVzdFxFLAogIGRlY2xhcmluZ0NsYXNzOiB7aXMgZGV2LmltcHJleC5vcmViZnVzY2F0b3IucmVmbGVjdC5kdW1teS5FbnRpdHl9LAogIHR5cGU6IHtpcyB2b2lkfQp9");
  private static final String REQUIREMENTS_METHOD = decode(
      "ewogIHJlcXVpcmVkTW9kaWZpZXJzOiBwcml2YXRlIHN5bmNocm9uaXplZCwKICBiYW5uZWRNb2RpZmllcnM6IHN0YXRpYywKICBpbmNsdWRlU3ludGhldGljLAogIG5hbWU6IFxRdGVzdFxFLAogIGRlY2xhcmluZ0NsYXNzOiB7aXMgZGV2LmltcHJleC5vcmViZnVzY2F0b3IucmVmbGVjdC5kdW1teS5FbnRpdHl9LAogIGV4Y2VwdGlvbkNsYXNzOiB7CiAgICA8YW55Pj17c3ViLWNsYXNzLW9mIGphdmEubGFuZy5SdW50aW1lRXhjZXB0aW9ufSwKICAgIDA9e2lzIGphdmEubGFuZy5OdWxsUG9pbnRlckV4Y2VwdGlvbn0KICB9LAogIHBhcmFtZXRlckNsYXNzOiB7CiAgICA8YW55Pj17aXMgaW50fSwKICAgIDA9e2lzIGludH0sCiAgICAyPXtzdXBlci1jbGFzcy1vZiBkZXYuaW1wcmV4Lm9yZWJmdXNjYXRvci5yZWZsZWN0LmR1bW15LkVudGl0eX0sCiAgICA0PXtyZWdleCAuKlR5cGV9CiAgfSwKICBwYXJhbWV0ZXJDb3VudDogNiwKICByZXR1cm5UeXBlOiB7aXMgdm9pZH0KfQ==");

  private static String decode(String input) {
    return new String(Base64.getDecoder().decode(input), StandardCharsets.UTF_8);
  }

  @Test
  void testRecursive() {
    Reflector reflector = Reflector.of(Player.class)
        .recursive();

    assertEquals(4, reflector.constructor()
        .stream().count());
    assertEquals(1, reflector.constructor()
        .declaringClass().is(Object.class)
        .stream().count());
  }

  @Test
  void testRecursiveUntil() {
    Reflector reflector = Reflector.of(Player.class)
        .recursiveUntil(Entity.class);

    assertEquals(3, reflector.constructor()
        .stream().count());
    assertEquals(0, reflector.constructor()
        .declaringClass().is(Object.class)
        .stream().count());
  }

  @Test
  void testRequirementFormatting() {
    Reflector reflector = Reflector.of(Entity.class);

    var exception = assertThrows(IllegalArgumentException.class, () -> {
      reflector.field()
          .requireModifier(Modifier.PRIVATE | Modifier.SYNCHRONIZED)
          .banStatic()
          .declaringClass().is(Entity.class)
          .nameIs("test")
          .type().is(void.class)
          .includeSynthetic()
          .firstOrThrow();
    });
    assertTrue(exception.getMessage().endsWith(REQUIREMENTS_FIELD));

    exception = assertThrows(IllegalArgumentException.class, () -> {
      reflector.method()
          .requireModifier(Modifier.PRIVATE | Modifier.SYNCHRONIZED)
          .banStatic()
          .declaringClass().is(Entity.class)
          .nameIs("test")
          .exception(0).is(NullPointerException.class)
          .exception().subOf(RuntimeException.class)
          .parameter().is(int.class)
          .parameter(2).superOf(Entity.class)
          .parameter(0).is(int.class)
          .parameter(4).regex(Pattern.compile(".*Type", Pattern.DOTALL))
          .returnType().is(void.class)
          .includeSynthetic()
          .parameterCount(6)
          .firstOrThrow();
    });
    assertTrue(exception.getMessage().endsWith(REQUIREMENTS_METHOD));
  }
}
