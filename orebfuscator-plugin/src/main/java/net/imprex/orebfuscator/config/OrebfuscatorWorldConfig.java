package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import com.lishid.orebfuscator.Orebfuscator;

import net.imprex.orebfuscator.util.WeightedRandom;

public class OrebfuscatorWorldConfig implements WorldConfig {

	private boolean enabled;
	private List<World> worlds = new ArrayList<>();
	private Set<Material> darknessMaterials = new HashSet<>();
	private Set<Material> hiddenMaterials = new HashSet<>();

	private WeightedRandom<Material> randomMaterials = new WeightedRandom<Material>();

	public void serialize(ConfigurationSection section) {
		this.enabled = section.getBoolean("enabled", true);

		this.worlds.clear();
		ConfigParser.readWorldList(section, this.worlds, "worlds");

		if (this.worlds.isEmpty()) {
			this.failSerialize("World config '" + section.getCurrentPath() + "' is missing 'worlds'");
			return;
		}

		this.darknessMaterials.clear();
		ConfigParser.readMaterialSet(section, this.darknessMaterials, "darknessMaterials");

		this.hiddenMaterials.clear();
		ConfigParser.readMaterialSet(section, this.hiddenMaterials, "hiddenMaterials");

		if (this.darknessMaterials.isEmpty() && this.hiddenMaterials.isEmpty()) {
			this.failSerialize("config '" + section.getCurrentPath() + "' is missing 'darknessMaterials' and 'hiddenMaterials' blocks");
			return;
		}

		this.randomMaterials.clear();
		ConfigParser.readRandomMaterialList(section, this.randomMaterials, "randomBlocks");

		if (this.randomMaterials.isEmpty()) {
			this.failSerialize("config section '" + section.getCurrentPath() + "' is missing 'randomBlocks'");
		}
	}

	private void failSerialize(String message) {
		this.enabled = false;
		Orebfuscator.LOGGER.warning(message);
	}

	@Override
	public Material randomMaterial() {
		return this.randomMaterials.next();
	}

	@Override
	public boolean enabled() {
		return this.enabled;
	}

	@Override
	public List<World> worlds() {
		return Collections.unmodifiableList(this.worlds);
	}

	@Override
	public Set<Material> darknessMaterials() {
		return Collections.unmodifiableSet(this.darknessMaterials);
	}

	@Override
	public Set<Material> hiddenMaterials() {
		return Collections.unmodifiableSet(this.hiddenMaterials);
	}
}
