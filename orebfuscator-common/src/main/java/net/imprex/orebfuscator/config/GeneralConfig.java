package net.imprex.orebfuscator.config;

public interface GeneralConfig {

	boolean checkForUpdates();

	boolean updateOnBlockDamage();

	boolean bypassNotification();
	
	boolean ignoreSpectator();

	int updateRadius();
}