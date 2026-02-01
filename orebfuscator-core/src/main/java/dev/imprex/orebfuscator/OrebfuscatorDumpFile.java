package dev.imprex.orebfuscator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.util.JavaVersion;
import java.io.BufferedWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class OrebfuscatorDumpFile extends ConfigurationSection {

  private static final DateTimeFormatter FILE_FORMAT = DateTimeFormatter.ofPattern("uuuu-MM-dd_HH.mm.ss");
  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting()
      .registerTypeAdapter(ConfigurationSection.class, new Json())
      .create();

  private final OrebfuscatorCore orebfuscator;
  private final TemporalAccessor now;

  public OrebfuscatorDumpFile(OrebfuscatorCore orebfuscator) {
    super("");

    this.orebfuscator = orebfuscator;
    this.now = OffsetDateTime.now(ZoneOffset.UTC);

    this.initialize();
  }

  private void initialize() {
    set("timestamp", TIME_FORMAT.format(now));

    set("versions.java", JavaVersion.get());
    set("versions.orebfuscator", orebfuscator.orebfuscatorVersion().toString());

    var statistics = createSection("statistics");
    orebfuscator.statisticsRegistry().entries().stream()
        .sorted(Comparator.comparing(Map.Entry::getKey))
        .forEach(entry -> statistics.set(entry.getKey(), entry.getValue()));

    var levels = createSection("levels");
    for (WorldAccessor accessor : orebfuscator.worlds()) {
      var section = levels.createSection(accessor.name());
      section.set("accessor", accessor);
      section.set("minY", accessor.minBuildHeight());
      section.set("maxY", accessor.maxBuildHeight());
    }

    orebfuscator.config().dumpBlocks(createSection("blocks"));

    Base64.Encoder encoder = Base64.getUrlEncoder();
    String latestLog = OfcLogger.getLatestVerboseLog();
    set("verboseLog", encoder.encodeToString(latestLog.getBytes(StandardCharsets.UTF_8)));

    try {
      Path configPath = orebfuscator.configDirectory().resolve("config.yml");
      String config = String.join("\n", Files.readAllLines(configPath));
      set("config", encoder.encodeToString(config.getBytes(StandardCharsets.UTF_8)));
    } catch (IOException e) {
      OfcLogger.error(e);
    }

    String configReport = orebfuscator.config().report();
    configReport = configReport != null ? configReport : "";
    set("configReport", encoder.encodeToString(configReport.getBytes(StandardCharsets.UTF_8)));
  }

  public Path write() {
    Path path = orebfuscator.configDirectory().resolve("dump-" + FILE_FORMAT.format(now) + ".json");
    try (BufferedWriter writer = Files.newBufferedWriter(path)) {
      GSON.toJson(this, writer);
    } catch (IOException e) {
      OfcLogger.error(e);
    }

    return path;
  }

  private static class Json implements JsonSerializer<ConfigurationSection> {

    @Override
    public JsonElement serialize(ConfigurationSection section, Type type, JsonSerializationContext jsonSerializationContext) {
      return serialize(section);
    }

    private JsonElement serialize(Object value) {
      if (value instanceof Boolean booleanValue) {
        return new JsonPrimitive(booleanValue);
      } else if (value instanceof Number numberValue) {
        return new JsonPrimitive(numberValue);
      } else if (value instanceof String stringValue) {
        return new JsonPrimitive(stringValue);
      } else if (value instanceof List<?> list) {
        JsonArray json = new JsonArray(list.size());
        for (Object item : list) {
          json.add(serialize(item));
        }
        return json;
      } else if (value instanceof ConfigurationSection section) {
        JsonObject json = new JsonObject();
        for (String key : section.getKeys()) {
          json.add(key, serialize(section.get(key)));
        }
        return json;
      }

      throw new JsonIOException("Unexpected type " + value.getClass());
    }
  }
}
