package dev.imprex.orebfuscator.config.api;

public interface GeneralConfig {

  boolean checkForUpdates();

  boolean updateOnBlockDamage();

  boolean bypassNotification();

  boolean ignoreSpectator();

  int updateRadius();
}