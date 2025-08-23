package dev.imprex.orebfuscator.config.yaml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class YamlConfigurationTest {

  private static final String SEQUENCE_COMMENT_EXAMPLE = "#header\nlist:\n- a # inline a\n- b # inline b\n- c #inline c\n#footer\n";

  @Test
  void testSaveEmpty() throws IOException {
    assertEquals("", new YamlConfiguration().withoutComments());
  }

  @Test
  void testInvalidConfig() throws IOException, InvalidConfigurationException {
    try (InputStream inputStream = toInputStream("\tboolean: true")) {
      assertThrows(InvalidConfigurationException.class, () -> YamlConfiguration.loadConfig(inputStream));
    }

    try (InputStream inputStream = toInputStream("- a\n- b\n")) {
      assertThrows(InvalidConfigurationException.class, () -> YamlConfiguration.loadConfig(inputStream));
    }

    try (InputStream inputStream = toInputStream("key.with.path: 1")) {
      assertThrows(InvalidConfigurationException.class, () -> YamlConfiguration.loadConfig(inputStream));
    }

    try (InputStream inputStream = toInputStream("root: \n  '': 1")) {
      assertThrows(InvalidConfigurationException.class, () -> YamlConfiguration.loadConfig(inputStream));
    }

    try (InputStream inputStream = toInputStream("root: \n  '\t': 1")) {
      assertThrows(InvalidConfigurationException.class, () -> YamlConfiguration.loadConfig(inputStream));
    }
  }

  @Test
  void testSerializationRoundTrip(@TempDir Path tempDir) throws IOException, InvalidConfigurationException {
    YamlConfiguration original = new YamlConfiguration();

    original.set("boolean", true);
    original.set("child.int", -1);
    original.set("child.double", -1.2d);
    original.set("strings.foo", "foo");
    original.set("strings.bar", "bar");
    original.set("strings.baz.qux", "qux");
    original.set("list", List.of("a", "b", "c", "d"));

    Path filePath = tempDir.resolve("test-serialization-round-trip.yml");
    original.save(filePath);

    YamlConfiguration read = YamlConfiguration.loadConfig(filePath);
    assertEquals(original, read);
  }

  @Test
  void testCommentPreservation(@TempDir Path tempDir) throws IOException, InvalidConfigurationException {
    Path expectedPath = Paths.get("src/test/resources/config/example-config.yml");
    YamlConfiguration configuration = YamlConfiguration.loadConfig(expectedPath);

    Path actualPath = tempDir.resolve("test-deserialization-round-trip.yml");
    configuration.save(actualPath);

    assertEquals(Files.readString(expectedPath), Files.readString(actualPath));
  }

  @Test
  void testSequenceCommentPreservation(@TempDir Path tempDir) throws IOException, InvalidConfigurationException {
    try (InputStream inputStream = toInputStream(SEQUENCE_COMMENT_EXAMPLE)) {
      YamlConfiguration configuration = YamlConfiguration.loadConfig(inputStream);
      configuration.set("list", List.of("a", "c"));

      Path actualPath = tempDir.resolve("test-sequence-comment-preservation.yml");
      configuration.save(actualPath);

      assertEquals("#header\nlist:\n- a # inline a\n- c #inline c\n#footer\n", Files.readString(actualPath));
    }
  }

  @Test
  void testWithoutComments() throws IOException, InvalidConfigurationException {
    try (InputStream inputStream = toInputStream(SEQUENCE_COMMENT_EXAMPLE)) {
      YamlConfiguration configuration = YamlConfiguration.loadConfig(inputStream);
      assertEquals("list:\n- a\n- b\n- c\n", configuration.withoutComments());
    }
  }

  private static InputStream toInputStream(String value) {
    return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
  }
}
