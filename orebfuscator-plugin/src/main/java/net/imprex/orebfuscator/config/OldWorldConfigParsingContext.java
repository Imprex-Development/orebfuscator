package net.imprex.orebfuscator.config;

import net.imprex.orebfuscator.util.OFCLogger;
import org.bukkit.configuration.ConfigurationSection;

public interface OldWorldConfigParsingContext {

    default void warnUnknownBlock(ConfigurationSection section, String path, String name) {
        OFCLogger.warn(String.format("config section '%s.%s' contains unknown block '%s', skipping",
                section.getCurrentPath(), path, name));
    }

    default int warnMinMaxValue(int value, int min, int max, String section, String path) {
        if (value < min || value > max) {
            OFCLogger.warn(String.format("value=%s for '%s.%s' needs to be less then %s and greater then %s",
                    value, section, path, max, min));
        }
        return Math.min(max, Math.max(min, value));
    }

    void fail(String reason);

    default void failMissingOrEmpty(ConfigurationSection section, String missingSection) {
        this.fail(String.format("config section '%s.%s' is missing or empty",
                section.getCurrentPath(), missingSection));
    }
}
