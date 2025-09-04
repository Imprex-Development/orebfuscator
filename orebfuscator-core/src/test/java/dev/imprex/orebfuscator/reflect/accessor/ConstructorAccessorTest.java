package dev.imprex.orebfuscator.reflect.accessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class ConstructorAccessorTest {

  @Test
  @SuppressWarnings("deprecation")
  void testWrapConstructor() throws Exception {
    var constructor = ConstructorTest.class.getDeclaredConstructor(String.class);
    assertFalse(constructor.isAccessible());

    var accessor = Accessors.wrap(constructor);

    assertFalse(constructor.isAccessible());
    assertEquals(constructor, accessor.member());
  }

  @Test
  void testInvoke() throws Exception {
    var constructor = ConstructorTest.class.getDeclaredConstructor(String.class);
    var accessor = Accessors.wrap(constructor);

    var instance = (ConstructorTest) accessor.invoke("a");
    assertEquals(List.of("a"), instance.values);

    assertThrows(IllegalStateException.class, () -> accessor.invoke(1));
  }

  @Test
  void testInvokeVarargs() throws Exception {
    var constructor = ConstructorTest.class.getDeclaredConstructor(String[].class);
    var accessor = Accessors.wrap(constructor);

    var values = new String[]{"a", "b", "c"};
    var instance = (ConstructorTest) accessor.invoke(new Object[]{values});
    assertEquals(List.of(values), instance.values);

    assertThrows(IllegalStateException.class, () -> accessor.invoke(1));
  }

  public static class ConstructorTest {

    final List<String> values;

    public ConstructorTest(String value) {
      this.values = List.of(value);
    }

    public ConstructorTest(String... values) {
      this.values = List.of(values);
    }
  }
}
