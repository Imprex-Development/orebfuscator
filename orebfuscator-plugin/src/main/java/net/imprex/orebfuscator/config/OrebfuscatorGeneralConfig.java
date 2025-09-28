package net.imprex.orebfuscator.config;

import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.config.context.ConfigParsingContext;

public class OrebfuscatorGeneralConfig implements GeneralConfig {

  private boolean checkForUpdates = true;
  private boolean updateOnBlockDamage = true;
  private boolean bypassNotification = true;
  private boolean ignoreSpectator = false;
  private int updateRadius = 2;

  public void deserialize(ConfigurationSection section, ConfigParsingContext context) {
    this.checkForUpdates = section.getBoolean("checkForUpdates", true);
    this.updateOnBlockDamage = section.getBoolean("updateOnBlockDamage", true);
    this.bypassNotification = section.getBoolean("bypassNotification", true);
    this.ignoreSpectator = section.getBoolean("ignoreSpectator", false);

    this.updateRadius = section.getInt("updateRadius", 2);
    context.errorMinValue("updateRadius", 1, this.updateRadius);
  }

  public void serialize(ConfigurationSection section) {
    section.set("checkForUpdates", this.checkForUpdates);
    section.set("updateOnBlockDamage", this.updateOnBlockDamage);
    section.set("bypassNotification", this.bypassNotification);
    section.set("ignoreSpectator", this.ignoreSpectator);
    section.set("updateRadius", this.updateRadius);
  }

  @Override
  public boolean checkForUpdates() {
    return this.checkForUpdates;
  }

  @Override
  public boolean updateOnBlockDamage() {
    return this.updateOnBlockDamage;
  }

  @Override
  public boolean bypassNotification() {
    return this.bypassNotification;
  }

  @Override
  public boolean ignoreSpectator() {
    return this.ignoreSpectator;
  }

  @Override
  public int updateRadius() {
    return this.updateRadius;
  }
}
