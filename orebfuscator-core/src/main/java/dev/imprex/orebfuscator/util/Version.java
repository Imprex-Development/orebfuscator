package dev.imprex.orebfuscator.util;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public record Version(int major, int minor, int patch) implements Comparable<Version> {

  private static final Pattern VERSION_PATTERN =
      Pattern.compile("(?<major>\\d+)(?:\\.(?<minor>\\d+))?(?:\\.(?<patch>\\d+))?");

  public static Version parse(String version) {
    return tryParse(version)
        .orElseThrow(() -> new IllegalArgumentException("Can't parse version: " + version));
  }

  public static Optional<Version> tryParse(String version) {
    Matcher matcher = VERSION_PATTERN.matcher(version);

    if (!matcher.find()) {
      return Optional.empty();
    }

    int major = Integer.parseInt(matcher.group("major"));

    String minorGroup = matcher.group("minor");
    int minor = minorGroup != null ? Integer.parseInt(minorGroup) : 0;

    String patchGroup = matcher.group("patch");
    int patch = patchGroup != null ? Integer.parseInt(patchGroup) : 0;

    return Optional.of(new Version(major, minor, patch));
  }

  public boolean isAbove(String version) {
    return this.isAbove(Version.parse(version));
  }

  public boolean isAbove(Version version) {
    return this.compareTo(version) > 0;
  }

  public boolean isAtOrAbove(String version) {
    return this.isAtOrAbove(Version.parse(version));
  }

  public boolean isAtOrAbove(Version version) {
    return this.compareTo(version) >= 0;
  }

  public boolean isAtOrBelow(String version) {
    return this.isAtOrBelow(Version.parse(version));
  }

  public boolean isAtOrBelow(Version version) {
    return this.compareTo(version) <= 0;
  }

  public boolean isBelow(String version) {
    return this.isBelow(Version.parse(version));
  }

  public boolean isBelow(Version version) {
    return this.compareTo(version) < 0;
  }

  @Override
  public int compareTo(Version other) {
    int major = Integer.compare(this.major, other.major);
    if (major != 0) {
      return major;
    }

    int minor = Integer.compare(this.minor, other.minor);
    if (minor != 0) {
      return minor;
    }

    return Integer.compare(this.patch, other.patch);
  }

  @Override
  public int hashCode() {
    return Objects.hash(major, minor, patch);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Version other)) {
      return false;
    }
    return major == other.major && minor == other.minor && patch == other.patch;
  }

  @Override
  public String toString() {
    return String.format("%s.%s.%s", this.major, this.minor, this.patch);
  }

  public static final class Json extends TypeAdapter<Version> {

    @Override
    public void write(JsonWriter out, Version value) throws IOException {
      out.value(value.toString());
    }

    @Override
    public Version read(JsonReader in) throws IOException {
      return Version.parse(in.nextString());
    }
  }
}
