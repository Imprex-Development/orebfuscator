package net.imprex.orebfuscator.config;

import org.bukkit.configuration.ConfigurationSection;

public class OrebfuscatorGeneralConfig implements GeneralConfig {

	private boolean updateOnBlockDamage = true;
	private boolean bypassNotification = true;
	private int initialRadius = 1;
	private int updateRadius = 2;

	public void serialize(ConfigurationSection section) {
		this.updateOnBlockDamage = section.getBoolean("updateOnBlockDamage", true);
		this.bypassNotification = section.getBoolean("bypassNotification", true);
		this.initialRadius = section.getInt("initialRadius", 1);
		this.updateRadius = section.getInt("updateRadius", 2);
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
	public int initialRadius() {
		return this.initialRadius;
	}

	@Override
	public int updateRadius() {
		return this.updateRadius;
	}
}
