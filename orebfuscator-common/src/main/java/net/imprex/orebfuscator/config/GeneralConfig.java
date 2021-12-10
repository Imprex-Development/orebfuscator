package net.imprex.orebfuscator.config;

public interface GeneralConfig {

	boolean checkForUpdates();
	void checkForUpdates(boolean enabled);

	boolean updateOnBlockDamage();
	void updateOnBlockDamage(boolean enabled);

	boolean bypassNotification();
	void bypassNotification(boolean enabled);
	
	boolean ignoreSpectator();
	void ignoreSpectator(boolean value);

	int updateRadius();
	void updateRadius(int radius);
}