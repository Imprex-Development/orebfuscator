/*
 * This code is adapted from the Bukkit/Spigot project:
 * https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/browse/src/main/java/org/bukkit/configuration/InvalidConfigurationException.java
 * Copyright (C) 2011-2024 Bukkit Project (original authors and contributors)
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package dev.imprex.orebfuscator.config.yaml;

public class InvalidConfigurationException extends Exception {

  public InvalidConfigurationException(String message) {
    super(message);
  }

  public InvalidConfigurationException(Throwable cause) {
    super(cause);
  }

  public InvalidConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
