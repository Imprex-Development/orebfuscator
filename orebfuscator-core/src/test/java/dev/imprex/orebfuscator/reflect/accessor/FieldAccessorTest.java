package dev.imprex.orebfuscator.reflect.accessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FieldAccessorTest {

  @Test
  @SuppressWarnings("deprecation")
  void testWrapField() throws Exception {
    var field = FieldTest.class.getDeclaredField("value");
    assertFalse(field.isAccessible());

    var accessor = Accessors.wrap(field);

    assertFalse(field.isAccessible());
    assertEquals(field, accessor.member());
  }

  @Test
  @SuppressWarnings("deprecation")
  void testWrapStaticField() throws Exception {
    var field = FieldTest.class.getDeclaredField("staticValue");
    assertFalse(field.isAccessible());

    var accessor = Accessors.wrap(field);

    assertFalse(field.isAccessible());
    assertEquals(field, accessor.member());
  }

  @Test
  void testGet() throws Exception {
    var field = FieldTest.class.getDeclaredField("value");
    var accessor = Accessors.wrap(field);

    var instance = new FieldTest(1);
    assertEquals(instance.value, accessor.get(instance));
    assertThrows(IllegalStateException.class, () -> accessor.get(null));
  }

  @Test
  void testSet() throws Exception {
    var field = FieldTest.class.getDeclaredField("value");
    var accessor = Accessors.wrap(field);

    var instance = new FieldTest(1);
    accessor.set(instance, -1);
    assertEquals(-1, instance.value);
    assertThrows(IllegalStateException.class, () -> accessor.set(null, -1));
  }

  @Test
  void testStaticGet() throws Exception {
    var field = FieldTest.class.getDeclaredField("staticValue");
    var accessor = Accessors.wrap(field);

    FieldTest.staticValue = 1;
    assertEquals(FieldTest.staticValue, accessor.get(null));
    assertEquals(FieldTest.staticValue, accessor.get("ab"));
  }

  @Test
  void testStaticSet() throws Exception {
    var field = FieldTest.class.getDeclaredField("staticValue");
    var accessor = Accessors.wrap(field);

    accessor.set(null, -1);
    assertEquals(-1, FieldTest.staticValue);

    accessor.set("ab", -2);
    assertEquals(-2, FieldTest.staticValue);
  }

  public static class FieldTest {

    public static int staticValue;

    public int value;

    public FieldTest(int value) {
      this.value = value;
    }
  }
}
