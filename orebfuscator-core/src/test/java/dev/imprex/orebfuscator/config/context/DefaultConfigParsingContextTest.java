package dev.imprex.orebfuscator.config.context;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class DefaultConfigParsingContextTest {

  @Test
  void testNoErrorsNoWarnings() {
    DefaultConfigParsingContext context = new DefaultConfigParsingContext();

    assertFalse(context.hasErrors());
    assertNull(context.report());
  }

  @Test
  void testWarnDoesNotSetHasErrors() {
    DefaultConfigParsingContext context = new DefaultConfigParsingContext();
    context.warn(ConfigMessage.MISSING_OR_EMPTY);

    assertFalse(context.hasErrors());
    assertNotNull(context.report());
  }

  @Test
  void testErrorSetsHasErrors() {
    DefaultConfigParsingContext context = new DefaultConfigParsingContext();
    context.error(ConfigMessage.MISSING_OR_EMPTY);

    assertTrue(context.hasErrors());
    assertNotNull(context.report());
  }

  @Test
  void testErrorMinValue() {
    // value below min
    DefaultConfigParsingContext context = new DefaultConfigParsingContext();
    context.errorMinValue("value", 10, 5);
    assertTrue(context.hasErrors());

    // value equals min
    DefaultConfigParsingContext context2 = new DefaultConfigParsingContext();
    context2.errorMinValue("value", 10, 10);
    assertFalse(context2.hasErrors());

    // value above min
    DefaultConfigParsingContext context3 = new DefaultConfigParsingContext();
    context3.errorMinValue("value", 10, 15);
    assertFalse(context3.hasErrors());
  }

  @Test
  void testErrorMinMaxValue() {
    // value below allowed range
    DefaultConfigParsingContext context = new DefaultConfigParsingContext();
    context.errorMinMaxValue("value", 5, 10, 3);
    assertTrue(context.hasErrors());

    // value within range (min-bound)
    DefaultConfigParsingContext context2 = new DefaultConfigParsingContext();
    context2.errorMinMaxValue("value", 5, 10, 5);
    assertFalse(context2.hasErrors());

    // value within range
    DefaultConfigParsingContext context3 = new DefaultConfigParsingContext();
    context3.errorMinMaxValue("value", 5, 10, 7);
    assertFalse(context3.hasErrors());

    // value within range (max-bound)
    DefaultConfigParsingContext context4 = new DefaultConfigParsingContext();
    context4.errorMinMaxValue("value", 5, 10, 10);
    assertFalse(context4.hasErrors());

    // value above allowed range
    DefaultConfigParsingContext context5 = new DefaultConfigParsingContext();
    context5.errorMinMaxValue("value", 5, 10, 15);
    assertTrue(context5.hasErrors());
  }

  @Test
  void testNestedChildContextNonIsolated() {
    DefaultConfigParsingContext context = new DefaultConfigParsingContext();
    context.section("child").error(ConfigMessage.MISSING_OR_EMPTY);
    assertTrue(context.hasErrors());

    DefaultConfigParsingContext context2 = new DefaultConfigParsingContext();
    context2.error("child", ConfigMessage.MISSING_OR_EMPTY);
    assertTrue(context2.hasErrors());
  }

  @Test
  void testNestedChildContextIsolated() {
    DefaultConfigParsingContext context = new DefaultConfigParsingContext();
    ConfigParsingContext child = context.section("child", true);

    child.error(ConfigMessage.MISSING_OR_EMPTY);
    context.error("child.b", ConfigMessage.MISSING_OR_EMPTY);

    assertFalse(context.hasErrors());
    assertTrue(child.hasErrors());
  }

  @Test
  void testSectionPathValidation() {
    DefaultConfigParsingContext context = new DefaultConfigParsingContext();
    // Paths with blank (non-empty) segments (e.g. "\t" or "a. .b") should throw an exception.
    assertThrows(IllegalArgumentException.class, () -> context.section("\t"));
    assertThrows(IllegalArgumentException.class, () -> context.section("a.   .b"));
  }

  @Test
  void testDisableIfError() {
    DefaultConfigParsingContext context = new DefaultConfigParsingContext();
    assertTrue(context.disableIfError(true));
    assertFalse(context.disableIfError(false));

    context.error("child", ConfigMessage.MISSING_OR_EMPTY);
    assertFalse(context.disableIfError(true));
    assertFalse(context.disableIfError(false));

    // test if error isolation also works for disableIfError
    ConfigParsingContext child = context.section("child", true);
    assertTrue(context.disableIfError(true));
    assertFalse(child.disableIfError(true));
  }

  @Test
  void testReportWithNestedMessages() throws IOException {
    DefaultConfigParsingContext context = new DefaultConfigParsingContext();
    ConfigParsingContext child = context.section("child", true);

    // test all variations of warn
    context.warn(ConfigMessage.MISSING_OR_EMPTY);
    context.warn("a.b.c", ConfigMessage.MISSING_OR_EMPTY);
    child.warn(ConfigMessage.MISSING_OR_EMPTY);
    context.warn("child.a", ConfigMessage.MISSING_OR_EMPTY);

    // test all variations of error
    context.error(ConfigMessage.MISSING_OR_EMPTY);
    context.error("a.b.c", ConfigMessage.MISSING_OR_EMPTY);
    child.error(ConfigMessage.MISSING_OR_EMPTY);
    context.error("child.a", ConfigMessage.MISSING_OR_EMPTY);

    assertFalse(child.disableIfError(true));

    String expected = Files.readString(Paths.get("src/test/resources/config/context-test-report.txt"));
    assertEquals(expected, context.report());
  }
}
