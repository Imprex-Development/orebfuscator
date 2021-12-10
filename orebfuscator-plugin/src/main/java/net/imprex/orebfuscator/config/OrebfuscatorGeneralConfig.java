package net.imprex.orebfuscator.config;

import org.bukkit.configuration.ConfigurationSection;

public class OrebfuscatorGeneralConfig implements GeneralConfig {

	private boolean checkForUpdates = true;
	private boolean updateOnBlockDamage = true;
	private boolean bypassNotification = true;
	private boolean ignoreSpectator = false;
	private int updateRadius = 2;

	public void deserialize(ConfigurationSection section) {
		this.checkForUpdates(section.getBoolean("checkForUpdates", true));
		this.updateOnBlockDamage(section.getBoolean("updateOnBlockDamage", true));
		this.bypassNotification(section.getBoolean("bypassNotification", true));
		this.ignoreSpectator(section.getBoolean("ignoreSpectator", false));
		this.updateRadius(section.getInt("updateRadius", 2));
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
	public void checkForUpdates(boolean enabled) {
		this.checkForUpdates = enabled;
	}

	@Override
	public boolean updateOnBlockDamage() {
		return this.updateOnBlockDamage;
	}

	@Override
	public void updateOnBlockDamage(boolean enabled) {
		this.updateOnBlockDamage = enabled;
	}

	@Override
	public boolean bypassNotification() {
		return this.bypassNotification;
	}

	@Override
	public void bypassNotification(boolean enabled) {
		this.bypassNotification = enabled;
	}

	@Override
	public boolean ignoreSpectator() {
		return this.ignoreSpectator;
	}

	@Override
	public void ignoreSpectator(boolean value) {
		this.ignoreSpectator = value;
	}

	@Override
	public int updateRadius() {
		return this.updateRadius;
	}

	@Override
	public void updateRadius(int radius) {
		if (radius < 1) {
			throw new IllegalArgumentException("update radius must higher than zero");
		}
		this.updateRadius = radius;
	}
}
