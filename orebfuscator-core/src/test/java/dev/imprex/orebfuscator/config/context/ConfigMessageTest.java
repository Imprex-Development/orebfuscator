package dev.imprex.orebfuscator.config.context;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ConfigMessageTest {

  @Test
  void testFixedMessageFormat() {
    String formatted = ConfigMessage.MISSING_OR_EMPTY.format("ignored");
    assertEquals("is missing or empty", formatted);
  }

  @Test
  void testDynamicMessageFormat() {
    String formatted = ConfigMessage.VALUE_MIN.format(10, 5);
    assertEquals("value too low {value(10) < min(5)}", formatted);
  }
}
