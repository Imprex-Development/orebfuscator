package dev.imprex.orebfuscator.util;

import org.jetbrains.annotations.NotNull;

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
public record NamespacedKey(String namespace, String key) {

  /**
   * The namespace representing all inbuilt keys.
   */
  public static final String MINECRAFT = "minecraft";

  private static boolean isValidNamespaceChar(char c) {
    return (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '.' || c == '_' || c == '-';
  }

  private static boolean isValidKeyChar(char c) {
    return isValidNamespaceChar(c) || c == '/';
  }

  private static boolean isValidNamespace(String namespace) {
    int len = namespace.length();
    if (len == 0) {
      return false;
    }

    for (int i = 0; i < len; i++) {
      if (!isValidNamespaceChar(namespace.charAt(i))) {
        return false;
      }
    }

    return true;
  }

  private static boolean isValidKey(String key) {
    int len = key.length();
    if (len == 0) {
      return false;
    }

    for (int i = 0; i < len; i++) {
      if (!isValidKeyChar(key.charAt(i))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Create a key in a specific namespace.
   *
   * @param namespace namespace
   * @param key       key
   * @deprecated should never be used by plugins, for internal use only!!
   */
  @Deprecated
  public NamespacedKey(String namespace, String key) {
    if (namespace == null || !isValidNamespace(namespace)) {
      throw new IllegalArgumentException(String.format("Invalid namespace. Must be [a-z0-9._-]: %s", namespace));
    } else if (key == null || !isValidKey(key)) {
      throw new IllegalArgumentException(String.format("Invalid key. Must be [a-z0-9/._-]: %s", key));
    }

    this.namespace = namespace;
    this.key = key;

    String string = toString();
    if (string.length() >= 256) {
      throw new IllegalArgumentException(String.format("NamespacedKey must be less than 256 characters (%s)", string));
    }
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final NamespacedKey other = (NamespacedKey) obj;
    return this.namespace.equals(other.namespace) && this.key.equals(other.key);
  }

  @Override
  public String toString() {
    return this.namespace + ":" + this.key;
  }

  /**
   * Get a key in the Minecraft namespace.
   *
   * @param key the key to use
   * @return new key in the Minecraft namespace
   */
  public static NamespacedKey minecraft(String key) {
    return new NamespacedKey(MINECRAFT, key);
  }


  /**
   * Get a NamespacedKey from the supplied string.
   * <p>
   * The default namespace will be Minecraft's (i.e. {@link #minecraft(String)}).
   *
   * @return the created NamespacedKey. null if invalid
   */
  public static NamespacedKey fromString(@NotNull String string) {
    if (string == null || string.isEmpty()) {
      throw new IllegalArgumentException("Input string must not be empty or null");
    }

    String[] components = string.split(":", 3);
    if (components.length > 2) {
      return null;
    }

    String key = (components.length == 2) ? components[1] : "";
    if (components.length == 1) {
      String value = components[0];
      if (value.isEmpty() || !isValidKey(value)) {
        return null;
      }

      return minecraft(value);
    } else if (components.length == 2 && !isValidKey(key)) {
      return null;
    }

    String namespace = components[0];
    if (namespace.isEmpty()) {
      return minecraft(key);
    }

    if (!isValidNamespace(namespace)) {
      return null;
    }

    return new NamespacedKey(namespace, key);
  }
}
