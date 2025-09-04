package dev.imprex.orebfuscator.config.yaml;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

class ConfigurationSectionTest {

  @Test
  void testGetName() {
    ConfigurationSection section = new ConfigurationSection("");
    assertEquals("", section.getName());

    ConfigurationSection child = section.createSection("child");
    assertEquals("child", child.getName());
  }

  @Test
  void testGetKeys() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("a", 0);
    section.set("b.a", 0);
    section.set("b.b", 0);
    section.set("b.b.a", 0);

    assertArrayEquals(new String[]{"a", "b"}, section.getKeys().toArray());
  }

  @Test
  void testContains() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("a", 0);
    section.set("b.c", 0);
    section.set("c", null);

    assertTrue(section.contains("a"));
    assertTrue(section.contains("b"));
    assertFalse(section.contains("c"));

    assertFalse(section.contains("b.a"));
    assertFalse(section.contains("b.b"));
    assertTrue(section.contains("b.c"));
  }

  @Test
  void testSet() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("boolean", true);

    assertTrue(section.contains("boolean"));
    assertEquals(true, section.get("boolean"));

    section.set("boolean", null);

    assertFalse(section.contains("boolean"));
    assertNull(section.get("boolean"));
  }

  @Test
  void testSetNullDoesNotCreateSubSection() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("int.32", null);

    assertFalse(section.contains("int"));
    assertNull(section.get("int"));
  }

  @Test
  void testGet() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("boolean", true);
    section.set("int.32", Integer.MAX_VALUE);
    section.set("int.64", Long.MAX_VALUE);

    assertEquals(section, section.get(""));
    assertEquals(true, section.get("boolean"));
    assertEquals(Integer.MAX_VALUE, section.get("int.32"));
    assertEquals(Long.MAX_VALUE, section.get("int.64"));
  }

  @Test
  void testGetWithDefaultValue() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("boolean", true);
    assertEquals(true, section.get("boolean"));
    assertEquals(true, section.get("boolean", false));

    assertNull(section.get("string"));
    assertEquals("foo", section.get("string", "foo"));

    assertNull(section.get("int.32"));
    assertEquals(Integer.MAX_VALUE, section.get("int.32", Integer.MAX_VALUE));
  }

  @Test
  void testCreateSection() {
    ConfigurationSection section = new ConfigurationSection("");

    assertEquals(section, section.createSection(""));
    assertEquals(section.createSection("should.be.same.instance"), section.createSection("should.be.same.instance"));

    assertNotNull(section.createSection("int"));
    assertNotNull(section.createSection("foo.bar"));
    assertNotNull(section.createSection("foo.baz"));
  }

  @Test
  void testIsBoolean() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("boolean", true);
    section.set("booleanObject", Boolean.TRUE);
    section.set("other", 1);

    assertTrue(section.isBoolean("boolean"));
    assertTrue(section.isBoolean("booleanObject"));
    assertFalse(section.isBoolean("other"));
    assertFalse(section.isBoolean("unknown"));
  }

  @Test
  void testGetBoolean() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("boolean", true);
    section.set("booleanObject", Boolean.TRUE);
    section.set("other", 1);

    assertEquals(true, section.getBoolean("boolean"));
    assertEquals(true, section.getBoolean("booleanObject"));
    assertNull(section.getBoolean("other"));
    assertNull(section.getBoolean("unknown"));
  }

  @Test
  void testGetBooleanWithDefaultValue() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("boolean", true);
    section.set("booleanObject", Boolean.TRUE);
    section.set("other", 1);

    assertEquals(true, section.getBoolean("boolean", false));
    assertEquals(true, section.getBoolean("booleanObject", false));
    assertEquals(false, section.getBoolean("other", false));
    assertEquals(false, section.getBoolean("unknown", false));
  }

  @Test
  void testIsInt() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("int", 1);
    section.set("intObject", Integer.valueOf(1));
    section.set("number", 1f);
    section.set("other", true);

    assertTrue(section.isInt("int"));
    assertTrue(section.isInt("intObject"));
    assertFalse(section.isInt("number"));
    assertFalse(section.isInt("other"));
    assertFalse(section.isInt("unknown"));
  }

  @Test
  void testGetInt() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("int", 1);
    section.set("intObject", Integer.valueOf(1));
    section.set("number", 1f);
    section.set("other", true);

    assertEquals(1, section.getInt("int"));
    assertEquals(1, section.getInt("intObject"));
    assertEquals(1, section.getInt("number"));
    assertNull(section.getInt("other"));
    assertNull(section.getInt("unknown"));
  }

  @Test
  void testGetIntWithDefaultValue() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("int", 1);
    section.set("intObject", Integer.valueOf(1));
    section.set("number", 1f);
    section.set("other", true);

    assertEquals(1, section.getInt("int", -1));
    assertEquals(1, section.getInt("intObject", -1));
    assertEquals(1, section.getInt("number", -1));
    assertEquals(-1, section.getInt("other", -1));
    assertEquals(-1, section.getInt("unknown", -1));
  }

  @Test
  void testIsLong() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("long", 1L);
    section.set("longObject", Long.valueOf(1L));
    section.set("number", 1);
    section.set("other", true);

    assertTrue(section.isLong("long"));
    assertTrue(section.isLong("longObject"));
    assertFalse(section.isLong("number"));
    assertFalse(section.isLong("other"));
    assertFalse(section.isLong("unknown"));
  }

  @Test
  void testGetLong() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("long", 1L);
    section.set("longObject", Long.valueOf(1L));
    section.set("number", 1);
    section.set("other", true);

    assertEquals(1L, section.getLong("long"));
    assertEquals(1L, section.getLong("longObject"));
    assertEquals(1L, section.getLong("number"));
    assertNull(section.getLong("other"));
    assertNull(section.getLong("unknown"));
  }

  @Test
  void testGetLongWithDefaultValue() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("long", 1L);
    section.set("longObject", Long.valueOf(1L));
    section.set("number", 1);
    section.set("other", true);

    assertEquals(1L, section.getLong("long", -1L));
    assertEquals(1L, section.getLong("longObject", -1L));
    assertEquals(1L, section.getLong("number", -1L));
    assertEquals(-1L, section.getLong("other", -1L));
    assertEquals(-1L, section.getLong("unknown", -1L));
  }

  @Test
  void testIsDouble() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("double", 1d);
    section.set("doubleObject", Double.valueOf(1d));
    section.set("number", 1);
    section.set("other", true);

    assertTrue(section.isDouble("double"));
    assertTrue(section.isDouble("doubleObject"));
    assertFalse(section.isDouble("number"));
    assertFalse(section.isDouble("other"));
    assertFalse(section.isDouble("unknown"));
  }

  @Test
  void testGetDouble() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("double", 1d);
    section.set("doubleObject", Double.valueOf(1d));
    section.set("number", 1);
    section.set("other", true);

    assertEquals(1d, section.getDouble("double"));
    assertEquals(1d, section.getDouble("doubleObject"));
    assertEquals(1d, section.getDouble("number"));
    assertNull(section.getDouble("other"));
    assertNull(section.getDouble("unknown"));
  }

  @Test
  void testGetDoubleWithDefaultValue() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("double", 1d);
    section.set("doubleObject", Double.valueOf(1d));
    section.set("number", 1);
    section.set("other", true);

    assertEquals(1d, section.getDouble("double", -1d));
    assertEquals(1d, section.getDouble("doubleObject", -1d));
    assertEquals(1d, section.getDouble("number", -1d));
    assertEquals(-1d, section.getDouble("other", -1d));
    assertEquals(-1d, section.getDouble("unknown", -1d));
  }

  @Test
  void testIsNumber() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("int8", (byte) 1);
    section.set("int16", (short) 1);
    section.set("int32", 1);
    section.set("int64", 1L);
    section.set("float32", 1f);
    section.set("float64", 1d);
    section.set("other", true);

    assertTrue(section.isNumber("int8"));
    assertTrue(section.isNumber("int16"));
    assertTrue(section.isNumber("int32"));
    assertTrue(section.isNumber("int64"));
    assertTrue(section.isNumber("float32"));
    assertTrue(section.isNumber("float64"));
    assertFalse(section.isNumber("other"));
  }

  @Test
  void testIsString() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("string", "foo");
    section.set("number", 1);
    section.set("other", true);

    assertTrue(section.isString("string"));
    assertFalse(section.isString("number"));
    assertFalse(section.isString("other"));
    assertFalse(section.isString("unknown"));
  }

  @Test
  void testGetString() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("string", "foo");
    section.set("number", 1);
    section.set("other", true);

    assertEquals("foo", section.getString("string"));
    assertNull(section.getString("number"));
    assertNull(section.getString("other"));
    assertNull(section.getString("unknown"));
  }

  @Test
  void testGetStringWithDefaultValue() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("string", "foo");
    section.set("number", 1);
    section.set("other", true);

    assertEquals("foo", section.getString("string", "bar"));
    assertEquals("bar", section.getString("number", "bar"));
    assertEquals("bar", section.getString("other", "bar"));
    assertEquals("bar", section.getString("unknown", "bar"));
  }

  @Test
  void testIsList() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("list", List.of());
    section.set("other", true);

    assertTrue(section.isList("list"));
    assertFalse(section.isList("other"));
    assertFalse(section.isList("unknown"));
  }

  @Test
  void testGetList() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("list", List.of());
    section.set("other", true);

    assertEquals(List.of(), section.getList("list"));
    assertNull(section.getList("other"));
    assertNull(section.getList("unknown"));
  }

  @Test
  void testGetListWithDefaultValue() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("list", List.of());
    section.set("other", true);

    assertEquals(List.of(), section.getList("list", List.of(true)));
    assertEquals(List.of(true), section.getList("other", List.of(true)));
    assertEquals(List.of(true), section.getList("unknown", List.of(true)));
  }

  @Test
  void testGetStringList() {
    ConfigurationSection section = new ConfigurationSection("");

    section.set("list", List.of(
        true,
        (byte) 1,
        (short) 2,
        3,
        4L,
        5.5f,
        6.6d,
        'a',
        "foo",
        List.of()));
    section.set("other", true);

    assertEquals(List.of(
        "true",
        "1",
        "2",
        "3",
        "4",
        "5.5",
        "6.6",
        "a",
        "foo"), section.getStringList("list"));
    assertEquals(Collections.EMPTY_LIST, section.getStringList("other"));
    assertEquals(Collections.EMPTY_LIST, section.getStringList("unknown"));
  }

  @Test
  void testIsSection() {
    ConfigurationSection section = new ConfigurationSection("");

    section.createSection("section");
    section.set("other", true);

    assertTrue(section.isSection("section"));
    assertFalse(section.isSection("other"));
    assertFalse(section.isSection("unknown"));
  }

  @Test
  void testGetSection() {
    ConfigurationSection section = new ConfigurationSection("");

    ConfigurationSection child = section.createSection("section");
    section.set("other", true);

    assertEquals(child, section.getSection("section"));
    assertNull(section.getSection("other"));
    assertNull(section.getSection("unknown"));
  }

  @Test
  void testGetSubSection() {
    ConfigurationSection section = new ConfigurationSection("");

    ConfigurationSection sectionA = section.createSection("sectionA");
    ConfigurationSection sectionB = section.createSection("sectionB");
    section.set("other", true);

    assertEquals(List.of(sectionA, sectionB), section.getSubSections());
  }
}
