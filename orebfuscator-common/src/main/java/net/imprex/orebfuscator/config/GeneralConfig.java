package net.imprex.orebfuscator.config;

public interface GeneralConfig {

	boolean checkForUpdates();
	void checkForUpdates(boolean enabled);

	boolean updateOnBlockDamage();
	void updateOnBlockDamage(boolean enabled);

	boolean bypassNotification();
	void bypassNotification(boolean enabled);

	int initialRadius();
	/**
	 * @param radius
	 * @throws IllegalArgumentException When the radius value is negative
	 */
	void initialRadius(int radius);

	int updateRadius();
	/**
	 * @param radius
	 * @throws IllegalArgumentException When the radius value is lower than one
	 */
	void updateRadius(int radius);

	int proximityHiderRunnerSize();
	/**
	 * @param size
	 * @throws IllegalArgumentException When the count value is lower than one
	 */
	void proximityHiderRunnerSize(int size);
}