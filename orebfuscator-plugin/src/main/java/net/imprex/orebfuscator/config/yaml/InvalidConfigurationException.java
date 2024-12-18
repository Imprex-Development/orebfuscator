/*
 * This code is adapted from the Bukkit project:
 * https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/browse/src/main/java/org/bukkit/configuration/InvalidConfigurationException.java
 * Copyright (C) 2011-2024 Bukkit Project (original authors and contributors)
 * Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package net.imprex.orebfuscator.config.yaml;

@SuppressWarnings("serial")
public class InvalidConfigurationException extends Exception {

    public InvalidConfigurationException() {}

    public InvalidConfigurationException(String msg) {
        super(msg);
    }

    public InvalidConfigurationException(Throwable cause) {
        super(cause);
    }

    public InvalidConfigurationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
