package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.util.OFCLogger;
import net.imprex.orebfuscator.util.WeightedRandom;

public class OrebfuscatorWorldConfig implements WorldConfig {	

	private boolean enabled;
	private final List<String> worlds = new ArrayList<>();
	private final Set<Material> hiddenBlocks = new LinkedHashSet<>();

	private final Map<Material, Integer> randomBlocks = new LinkedHashMap<>();
	private final List<Integer> randomBlockIds = new ArrayList<>();
	private final WeightedRandom<Integer> randomMaterials = new WeightedRandom<>();

	protected void initialize() {
		this.randomMaterials.clear();
		for (Entry<Material, Integer> entry : this.randomBlocks.entrySet()) {
			int blockId = NmsInstance.getMaterialIds(entry.getKey()).iterator().next();
			this.randomMaterials.add(entry.getValue(), blockId);
			this.randomBlockIds.add(blockId);
		}
	}

	protected void serialize(ConfigurationSection section) {
		this.enabled(section.getBoolean("enabled", true));

		List<String> worldNameList = section.getStringList("worlds");
		if (worldNameList == null || worldNameList.isEmpty()) {
			this.failSerialize(
					String.format("config section '%s.worlds' is missing or empty", section.getCurrentPath()));
			return;
		}
		this.worlds.clear();
		this.worlds.addAll(worldNameList);

		this.serializeMaterialSet(section, this.hiddenBlocks, "hiddenBlocks");
		if (this.hiddenBlocks.isEmpty()) {
			this.failSerialize(String.format("config section '%s' is missing 'hiddenBlocks'",
					section.getCurrentPath()));
			return;
		}

		ConfigParser.serializeRandomMaterialList(section, this.randomBlocks, "randomBlocks");
		if (this.randomBlocks.isEmpty()) {
			this.failSerialize(
					String.format("config section '%s.randomBlocks' is missing or empty", section.getCurrentPath()));
		}
	}

	protected void deserialize(ConfigurationSection section) {
		section.set("enabled", this.enabled);
		section.set("worlds", this.worlds);

		ConfigParser.deserializeMaterialSet(section, this.hiddenBlocks, "hiddenBlocks");
		ConfigParser.deserializeRandomMaterialList(section, this.randomBlocks, "randomBlocks");
	}

	private void serializeMaterialSet(ConfigurationSection section, Set<Material> materials, String path) {
		materials.clear();

		List<String> materialNames = section.getStringList(path);
		if (materialNames == null || materialNames.isEmpty()) {
			return;
		}

		for (String name : materialNames) {
			Optional<Material> material = NmsInstance.getMaterialByName(name);

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

	@Override
	public boolean enabled() {
		return this.enabled;
	}

	@Override
	public void enabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public List<String> worlds() {
		return this.worlds;
	}

	@Override
	public Set<Material> hiddenBlocks() {
		return this.hiddenBlocks;
	}

	@Override
	public Set<Material> randomBlocks() {
		return this.randomBlocks.keySet();
	}

	@Override
	public List<Integer> randomBlockIds() {
		return this.randomBlockIds;
	}

	@Override
	public int randomBlockId() {
		return this.randomMaterials.next();
	}
}
