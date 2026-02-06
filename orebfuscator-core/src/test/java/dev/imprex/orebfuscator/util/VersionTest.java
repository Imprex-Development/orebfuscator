package dev.imprex.orebfuscator.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

public class VersionTest {

  @Test
  public void testParse() {
    assertEquals(new Version(1, 0, 0, null), Version.parse("1"));
    assertEquals(new Version(1, 2, 0, null), Version.parse("1.2"));
    assertEquals(new Version(1, 2, 3, null), Version.parse("1.2.3"));
    assertEquals(new Version(1, 2, 3, "-b0"), Version.parse("1.2.3-b0"));

    assertThrows(IllegalArgumentException.class, () -> Version.parse("a.1.c"));
    assertThrows(IllegalArgumentException.class, () -> Version.parse("a.b.c"));
    assertThrows(IllegalArgumentException.class, () -> Version.parse("fooBar"));
  }

  @Test
  public void testCompareTo() {
    Version a = new Version(1, 0, 0, null);
    Version b = new Version(1, 0, 0, null);
    Version c = new Version(1, 2, 0, null);
    Version d = new Version(1, 2, 3, null);
    Version e = new Version(2, 0, 0, "-b0");
    Version f = new Version(2, 0, 0, "-b1");
    Version g = new Version(2, 0, 0, null);

    assertFalse(a.isAbove(b));
    assertTrue(a.isAtOrAbove(b));
    assertTrue(a.isAtOrBelow(b));
    assertFalse(a.isBelow(b));

    assertFalse(a.isAbove(c));
    assertFalse(a.isAtOrAbove(c));
    assertTrue(a.isAtOrBelow(c));
    assertTrue(a.isBelow(c));

    assertTrue(c.isAbove(a));
    assertTrue(c.isAtOrAbove(a));
    assertFalse(c.isAtOrBelow(a));
    assertFalse(c.isBelow(a));

    assertFalse(c.isAbove(d));
    assertFalse(c.isAtOrAbove(d));
    assertTrue(c.isAtOrBelow(d));
    assertTrue(c.isBelow(d));

    assertFalse(d.isAbove(e));
    assertFalse(d.isAtOrAbove(e));
    assertTrue(d.isAtOrBelow(e));
    assertTrue(d.isBelow(e));

    assertFalse(e.isAbove(f));
    assertFalse(e.isAtOrAbove(f));
    assertTrue(e.isAtOrBelow(f));
    assertTrue(e.isBelow(f));

    assertFalse(f.isAbove(g));
    assertFalse(f.isAtOrAbove(g));
    assertTrue(f.isAtOrBelow(g));
    assertTrue(f.isBelow(g));

    List<Version> versions = new ArrayList<>(List.of(a, b, c, d, e, f, g));
    Collections.shuffle(versions);

    versions.sort(Comparator.naturalOrder());
    assertEquals(List.of(a, b, c, d, e, f, g), versions);

    versions.sort(Comparator.reverseOrder());
    assertEquals(List.of(g, f, e, d, c, b, a), versions);
  }

  @Test
  public void testHashCode() {
    Version a = new Version(1, 2, 3, "-b0");
    Version b = new Version(1, 2, 3, "-b0");

    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  public void testEquals() {
    Version a = new Version(1, 2, 3, "-b0");
    Version b = new Version(1, 2, 3, "-b0");
    Version c = new Version(1, 2, 3, "-b1");

    assertEquals(a, a);
    assertEquals(a, b);

    assertNotEquals(a, c);
    assertNotEquals(b, c);

    assertNotEquals(a, new Object());
  }

  @Test
  public void testToString() {
    assertEquals("1.0.0", new Version(1, 0, 0, null).toString());
    assertEquals("1.2.0", new Version(1, 2, 0, null).toString());
    assertEquals("1.2.3", new Version(1, 2, 3, null).toString());
    assertEquals("1.2.3-b0", new Version(1, 2, 3, "-b0").toString());
  }

  @Test
  public void testJson() {
    DummyClass dummy = new DummyClass(
        new Version(1, 0, 0, null),
        new Version(2, 3, 4, "-alpha"));

    JsonElement json = AbstractHttpService.GSON.toJsonTree(dummy);
    DummyClass other = AbstractHttpService.GSON.fromJson(json, DummyClass.class);

    assertEquals(dummy, other);
  }

  private record DummyClass(Version fieldA, Version fieldB) {
  }
}
