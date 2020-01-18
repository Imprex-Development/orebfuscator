package net.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import com.lishid.orebfuscator.Orebfuscator;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.util.WeightedRandom;

public class OrebfuscatorWorldConfig implements WorldConfig {	

	private boolean enabled;
	private List<World> worlds = new ArrayList<>();
	private Set<Material> hiddenBlocks = new HashSet<>();
	private Set<Material> darknessBlocks = new HashSet<>();

	private byte[] blockmask;

	private Map<Material, Integer> randomBlocks = new HashMap<>();
	private List<Integer> randomBlockIds = new ArrayList<>();
	private WeightedRandom<Integer> randomMaterials = new WeightedRandom<>();

	protected void initialize() {
		this.randomMaterials.clear();
		for (Entry<Material, Integer> entry : this.randomBlocks.entrySet()) {
			int blockId = NmsInstance.get().getMaterialIds(entry.getKey()).iterator().next();
			this.randomMaterials.add(entry.getValue(), blockId);
			this.randomBlockIds.add(blockId);
		}

		this.blockmask = new byte[NmsInstance.get().getMaterialSize()];
		for (Material material : this.hiddenBlocks) {
			this.setBlockmask(NmsInstance.get().getMaterialIds(material), false);
		}
		for (Material material : this.darknessBlocks) {
			this.setBlockmask(NmsInstance.get().getMaterialIds(material), true);
		}
	}

	private void setBlockmask(Set<Integer> blockIds, boolean isDarknessBlock) {
		for (int blockId : blockIds) {
			int blockmask = this.blockmask[blockId] | WorldConfig.BLOCK_MASK_OBFUSCATE;

			if (isDarknessBlock) {
				blockmask |= WorldConfig.BLOCK_MASK_DARKNESS;
			}

			if (NmsInstance.get().isTileEntity(blockId)) {
				blockmask |= WorldConfig.BLOCK_MASK_TILEENTITY;
			}

			this.blockmask[blockId] = (byte) blockmask;
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

		List<String> materialNameList = section.getStringList(path);
		if (materialNameList == null || materialNameList.isEmpty()) {
			return;
		}

		for (String materialName : materialNameList) {
			Material material = Material.matchMaterial(materialName);

			if (material == null) {
				Orebfuscator.LOGGER.warning(String.format("config section '%s.%s' contains unknown block '%s'",
						section.getCurrentPath(), path, materialName));
				continue;
			}

			materials.add(material);
		}
	}

	private void failSerialize(String message) {
		this.enabled = false;
		Orebfuscator.LOGGER.warning(message);
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
	public int blockmask(int id) {
		return this.blockmask[id];
	}

	@Override
	public boolean darknessBlocksEnabled() {
		return this.darknessBlocks.size() != 0;
	}

	@Override
	public Collection<Integer> randomBlocks() {
		return this.randomBlockIds;
	}

	@Override
	public int randomBlockId() {
		return this.randomMaterials.next();
	}
}
