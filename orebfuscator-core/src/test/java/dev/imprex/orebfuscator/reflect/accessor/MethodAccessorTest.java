package dev.imprex.orebfuscator.reflect.accessor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MethodAccessorTest {

  @Test
  @SuppressWarnings("deprecation")
  void testWrapMethod() throws Exception {
    var method = MethodTest.class.getMethod("sum", int.class, int.class);
    assertFalse(method.isAccessible());

    var accessor = Accessors.wrap(method);

    assertFalse(method.isAccessible());
    assertEquals(method, accessor.member());
  }

  @Test
  @SuppressWarnings("deprecation")  
  void testWrapStaticMethod() throws Exception {
    var method = MethodTest.class.getMethod("staticSum", int.class, int.class);
    assertFalse(method.isAccessible());

    var accessor = Accessors.wrap(method);

    assertFalse(method.isAccessible());
    assertEquals(method, accessor.member());
  }

  @Test
  void testInvoke() throws Exception {
    var method = MethodTest.class.getDeclaredMethod("sum", int.class, int.class);
    var accessor = Accessors.wrap(method);

    assertEquals(3, accessor.invoke(MethodTest.INSTANCE, 1, 2));
    assertThrows(IllegalStateException.class, () -> accessor.invoke(null, 1, 2));
  }

  @Test
  void testInvokeVarargs() throws Exception {
    var method = MethodTest.class.getDeclaredMethod("sum", int.class, int[].class);
    var accessor = Accessors.wrap(method);

    assertEquals(10, accessor.invoke(MethodTest.INSTANCE, 1, new int[] {2, 3, 4}));
    assertThrows(IllegalStateException.class, () -> accessor.invoke(null, 1, new int[] {2, 3, 4}));
  }

  @Test
  void testStaticInvoke() throws Exception {
    var method = MethodTest.class.getDeclaredMethod("staticSum", int.class, int.class);
    var accessor = Accessors.wrap(method);

    assertEquals(3, accessor.invoke(null, 1, 2));
    assertEquals(3, accessor.invoke("ab", 1, 2));
  }

  @Test
  void testStaticInvokeVarargs() throws Exception {
    var method = MethodTest.class.getDeclaredMethod("staticSum", int.class, int[].class);
    var accessor = Accessors.wrap(method);

    assertEquals(10, accessor.invoke(null, 1, new int[] {2, 3, 4}));
    assertEquals(10, accessor.invoke("ab", 1, new int[] {2, 3, 4}));
  }

  public static class MethodTest {

    public static final MethodTest INSTANCE = new MethodTest();

    public static int staticSum(int a, int b) {
      return a + b;
    }

    public static int staticSum(int a, int... ints) {
      return a + Arrays.stream(ints).sum();
    }

    public int sum(int a, int b) {
      return a + b;
    }

    public int sum(int a, int... ints) {
      return a + Arrays.stream(ints).sum();
    }
  }
}
