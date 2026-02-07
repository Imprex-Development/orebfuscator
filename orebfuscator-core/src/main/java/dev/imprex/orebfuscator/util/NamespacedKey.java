package dev.imprex.orebfuscator.util;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.NullMarked;

/**
 * Represents a String based key which consists of two components - a namespace and a key.
 * <p>
 * Namespaces may only contain lowercase alphanumeric characters, periods, underscores, and hyphens.
 * <p>
 * Keys may only contain lowercase alphanumeric characters, periods, underscores, hyphens, and forward slashes.
 *
 * @author org.bukkit.NamespacedKey from 1.19.4
 *
 */

@NullMarked
public record NamespacedKey(String namespace, String key) {

  private static final String DEFAULT_NAMESPACE = "minecraft";

  private static final Pattern PARSE = Pattern.compile("^(?:(?<namespace>[a-z0-9._-]+):)?(?<key>[a-z0-9/._-]+)$");
  private static final Predicate<String> VALID_NAMESPACE = Pattern.compile("^[a-z0-9._-]+$").asMatchPredicate();
  private static final Predicate<String> VALID_KEY = Pattern.compile("^[a-z0-9/._-]+$").asMatchPredicate();

  public static NamespacedKey parse(String value) {
    return tryParse(value).orElseThrow(() -> new IllegalArgumentException(
        "Invalid namespaced key. Must be /^([a-z0-9._-]+:)?[a-z0-9/._-]+$/: %s".formatted(value)));
  }

  public static Optional<NamespacedKey> tryParse(String value) {
    Matcher matcher = PARSE.matcher(value);
    if (!matcher.find()) {
      return Optional.empty();
    }

    String namespace = matcher.group("namespace");
    String key = matcher.group("key");

    if ("..".equals(namespace)) {
      return Optional.empty();
    }

    return Optional.of(new NamespacedKey(namespace == null ? DEFAULT_NAMESPACE : namespace, key));
  }

  @SuppressWarnings("ConstantConditions")
  public NamespacedKey {
    if (namespace == null || namespace.equals("..") || !VALID_NAMESPACE.test(namespace)) {
      throw new IllegalArgumentException("Invalid namespace. Must be [a-z0-9._-]: %s".formatted(namespace));
    }
    if (key == null || !VALID_KEY.test(key)) {
      throw new IllegalArgumentException("Invalid key. Must be [a-z0-9/._-]: %s".formatted(key));
    }
  }

  @Override
  public String toString() {
    return this.namespace + ":" + this.key;
  }
}
