package dev.imprex.orebfuscator.config;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import dev.imprex.orebfuscator.util.Version;

public class ConfigLookup {

  private static final Pattern FILENAME_PATTERN = Pattern.compile("config-(?<version>.*)\\.yml");

  public static Version getConfigVersion(Version version) throws IOException {
    List<Version> versions = discoverConfigs().stream()
        .map(FILENAME_PATTERN::matcher)
        .filter(Matcher::find)
        .map(m -> m.group("version"))
        .map(Version::tryParse)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .sorted(Comparator.reverseOrder())
        .toList();

    for (Version configVersion : versions) {
      if (version.isAtOrAbove(configVersion)) {
        return configVersion;
      }
    }

    return null;
  }
  
  public static InputStream loadConfig(Version version) {
    String path = String.format("/config/config-%s.yml", version);
    return ConfigLookup.class.getResourceAsStream(path);
  }

  private static List<String> discoverConfigs() throws IOException {
    var protectionDomain = ConfigLookup.class.getProtectionDomain();
    var codeSource = protectionDomain != null
        ? protectionDomain.getCodeSource()
        : null;
    var location = codeSource != null
        ? codeSource.getLocation()
        : null;

    if (location == null) {
      return Collections.emptyList();
    }

    if (location.getPath().endsWith(".jar")) {
      URI jar = URI.create("jar:" + location);
      try (FileSystem fileSystem = FileSystems.newFileSystem(jar, Map.of())) {
        Path configDir = fileSystem.getPath("/config/");
        if (!Files.isDirectory(configDir)) {
          return Collections.emptyList();
        }

        try (var stream = Files.list(configDir)) {
          return stream
              .map(configDir::relativize)
              .map(Path::toString)
              .toList();
        }
      }
    }

    Path jarDir = Paths.get(URI.create(location.toString()));
    if (!Files.isDirectory(jarDir)) {
      return Collections.emptyList();
    }

    // remap local IDE execution paths
    Path baseDir = jarDir.endsWith("build/classes/java/main/")
        ? jarDir.resolve("../../../resources/main/config").normalize()
        : jarDir.resolve("config/");

    if (!Files.isDirectory(baseDir)) {
      return Collections.emptyList();
    }

    try (var stream = Files.list(baseDir)) {
      return stream
          .map(baseDir::relativize)
          .map(Path::toString)
          .toList();
    }
  }
}
