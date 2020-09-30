package net.imprex.orebfuscator.config;

import org.bukkit.configuration.ConfigurationSection;

public class OrebfuscatorGeneralConfig implements GeneralConfig {

	private boolean updateOnBlockDamage = true;
	private boolean bypassNotification = true;
	private int initialRadius = 1;
	private int updateRadius = 2;
	private int proximityHiderRunnerSize = 4;

	public void serialize(ConfigurationSection section) {
		this.updateOnBlockDamage(section.getBoolean("updateOnBlockDamage", true));
		this.bypassNotification(section.getBoolean("bypassNotification", true));
		this.initialRadius(section.getInt("initialRadius", 1));
		this.updateRadius(section.getInt("updateRadius", 2));
		this.proximityHiderRunnerSize(section.getInt("proximityHiderRunnerSize", 4));
	}

	public void deserialize(ConfigurationSection section) {
		section.set("updateOnBlockDamage", this.updateOnBlockDamage);
		section.set("bypassNotification", this.bypassNotification);
		section.set("initialRadius", this.initialRadius);
		section.set("updateRadius", this.updateRadius);
		section.set("proximityHiderRunnerSize", this.proximityHiderRunnerSize);
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
	public int initialRadius() {
		return this.initialRadius;
	}

	@Override
	public void initialRadius(int radius) {
		if (radius < 1) {
			throw new IllegalArgumentException("update radius must higher than zero");
		}
		this.initialRadius = radius;
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

	@Override
	public int proximityHiderRunnerSize() {
		return this.proximityHiderRunnerSize;
	}

	@Override
	public void proximityHiderRunnerSize(int size) {
		if (size < 1) {
			throw new IllegalArgumentException("proximity hider runner size must higher than zero");
		}
		this.proximityHiderRunnerSize = size;
	}
}
