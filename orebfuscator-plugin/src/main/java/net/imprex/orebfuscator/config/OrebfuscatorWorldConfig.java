package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.util.OFCLogger;
import net.imprex.orebfuscator.util.WeightedRandom;

public class OrebfuscatorWorldConfig implements WorldConfig {	

	private boolean enabled;
	private final List<World> worlds = new ArrayList<>();
	private final Set<Material> hiddenBlocks = new HashSet<>();
	private final Set<Material> darknessBlocks = new HashSet<>();

	private final Map<Material, Integer> randomBlocks = new HashMap<>();
	private final List<Integer> randomBlockIds = new ArrayList<>();
	private final WeightedRandom<Integer> randomMaterials = new WeightedRandom<>();

	protected void initialize() {
		this.randomMaterials.clear();
		for (Entry<Material, Integer> entry : this.randomBlocks.entrySet()) {
			int blockId = NmsInstance.get().getMaterialIds(entry.getKey()).iterator().next();
			this.randomMaterials.add(entry.getValue(), blockId);
			this.randomBlockIds.add(blockId);
		}
	}

	protected void serialize(ConfigurationSection section) {
		this.enabled = section.getBoolean("enabled", true);

		ConfigParser.serializeWorldList(section, this.worlds, "worlds");
		if (this.worlds.isEmpty()) {
			this.failSerialize(
					String.format("config section '%s.worlds' is missing or empty", section.getCurrentPath()));
			return;
		}

		this.serializeMaterialSet(section, this.darknessBlocks, "darknessBlocks");
		this.serializeMaterialSet(section, this.hiddenBlocks, "hiddenBlocks");
		if (this.darknessBlocks.isEmpty() && this.hiddenBlocks.isEmpty()) {
			this.failSerialize(String.format("config section '%s' is missing 'darknessBlocks' and 'hiddenBlocks'",
					section.getCurrentPath()));
			return;
		}

		ConfigParser.serializeRandomMaterialList(section, this.randomBlocks, "randomBlocks");
		if (this.randomBlocks.isEmpty()) {
			this.failSerialize(
					String.format("config section '%s.randomBlocks' is missing or empty", section.getCurrentPath()));
		}
	}

	private void serializeMaterialSet(ConfigurationSection section, Set<Material> materials, String path) {
		materials.clear();

		List<String> materialNames = section.getStringList(path);
		if (materialNames == null || materialNames.isEmpty()) {
			return;
		}

		for (String name : materialNames) {
			Optional<Material> material = NmsInstance.get().getMaterialByName(name);

			if (!material.isPresent()) {
				OFCLogger.warn(String.format("config section '%s.%s' contains unknown block '%s'",
						section.getCurrentPath(), path, name));
				continue;
			}

			materials.add(material.get());
		}
	}

	private void failSerialize(String message) {
		this.enabled = false;
		OFCLogger.warn(message);
	}

	public Set<Material> getHiddenBlocks() {
		return hiddenBlocks;
	}

	public Set<Material> getDarknessBlocks() {
		return darknessBlocks;
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
	public boolean darknessBlocksEnabled() {
		return this.darknessBlocks.size() != 0;
	}

	@Override
	public List<Integer> randomBlocks() {
		return this.randomBlockIds;
	}

	@Override
	public int randomBlockId() {
		return this.randomMaterials.next();
	}
}
