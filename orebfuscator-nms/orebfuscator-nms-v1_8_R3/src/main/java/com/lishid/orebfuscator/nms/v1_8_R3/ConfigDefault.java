package com.lishid.orebfuscator.nms.v1_8_R3;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;

import com.google.common.collect.ImmutableList;
import com.lishid.orebfuscator.types.ConfigDefaults;

import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.IBlockData;

public class ConfigDefault extends ConfigDefaults {

	public ConfigDefault() {
		// Default World
		this.defaultProximityHiderBlockIds = this.convertMaterialsToIds(new Material[] { Material.DISPENSER,
				Material.MOB_SPAWNER, Material.CHEST, Material.HOPPER, Material.WORKBENCH, Material.FURNACE,
				Material.BURNING_FURNACE, Material.ENCHANTMENT_TABLE, Material.EMERALD_ORE, Material.ENDER_CHEST,
				Material.ANVIL, Material.TRAPPED_CHEST, Material.DIAMOND_ORE });

		this.defaultDarknessBlockIds = this
				.convertMaterialsToIds(new Material[] { Material.MOB_SPAWNER, Material.CHEST });

		this.defaultMode1BlockId = this.getMaterialIds(Material.STONE).iterator().next();
		this.defaultProximityHiderSpecialBlockId = this.getMaterialIds(Material.STONE).iterator().next();

		// The End
		this.endWorldRandomBlockIds = this
				.convertMaterialsToIds(new Material[] { Material.BEDROCK, Material.OBSIDIAN, Material.ENDER_STONE });

		this.endWorldObfuscateBlockIds = this.convertMaterialsToIds(new Material[] { Material.ENDER_STONE });

		this.endWorldMode1BlockId = this.getMaterialIds(Material.ENDER_STONE).iterator().next();
		this.endWorldRequiredObfuscateBlockIds = this.convertMaterialsToIds(new Material[] { Material.ENDER_STONE });

		// Nether World
		this.netherWorldRandomBlockIds = this.convertMaterialsToIds(new Material[] { Material.GRAVEL,
				Material.NETHERRACK, Material.SOUL_SAND, Material.NETHER_BRICK, Material.QUARTZ_ORE });

		this.netherWorldObfuscateBlockIds = this
				.convertMaterialsToIds(new Material[] { Material.NETHERRACK, Material.QUARTZ_ORE });

		this.netherWorldMode1BlockId = this.getMaterialIds(Material.NETHERRACK).iterator().next();

		this.netherWorldRequiredObfuscateBlockIds = this.convertMaterialsToIds(new Material[] { Material.NETHERRACK });

		// Normal World
		this.normalWorldRandomBlockIds = this.convertMaterialsToIds(new Material[] { Material.STONE,
				Material.COBBLESTONE, Material.WOOD, Material.GOLD_ORE, Material.IRON_ORE, Material.COAL_ORE,
				Material.LAPIS_ORE, Material.TNT, Material.MOSSY_COBBLESTONE, Material.OBSIDIAN, Material.DIAMOND_ORE,
				Material.REDSTONE_ORE, Material.CLAY, Material.EMERALD_ORE });

		this.normalWorldObfuscateBlockIds = this.convertMaterialsToIds(new Material[] { Material.GOLD_ORE,
				Material.IRON_ORE, Material.COAL_ORE, Material.LAPIS_ORE, Material.CHEST, Material.DIAMOND_ORE,
				Material.ENDER_CHEST, Material.REDSTONE_ORE, Material.CLAY, Material.EMERALD_ORE });

		this.normalWorldMode1BlockId = this.getMaterialIds(Material.STONE).iterator().next();

		this.normalWorldRequiredObfuscateBlockIds = this.convertMaterialsToIds(new Material[] { Material.STONE });
	}

	@SuppressWarnings("deprecation")
	public Set<Integer> getMaterialIds(Material material) {
		Set<Integer> ids = new HashSet<>();
		int blockId = material.getId() << 4;
		Block block = Block.getById(material.getId());
		ImmutableList<IBlockData> blockDataList = block.P().a();

		for (IBlockData blockData : blockDataList) {
			ids.add(blockId | block.toLegacyData(blockData));
		}

		return ids;
	}

	private Set<Integer> convertMaterialsToSet(Material[] materials) {
		Set<Integer> ids = new HashSet<>();

		for (Material material : materials) {
			ids.addAll(this.getMaterialIds(material));
		}

		return ids;
	}

	private int[] convertMaterialsToIds(Material[] materials) {
		Set<Integer> ids = this.convertMaterialsToSet(materials);

		int[] result = new int[ids.size()];
		int index = 0;

		for (int id : ids) {
			result[index++] = id;
		}

		return result;
	}
}