package net.imprex.orebfuscator.nms.v1_15_R1;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_15_R1.block.data.CraftBlockData;

import com.lishid.orebfuscator.types.ConfigDefaults;

import net.minecraft.server.v1_15_R1.Block;

public class ConfigDefault extends ConfigDefaults {

	protected final HashMap<Material, Set<Integer>> materialIds = new HashMap<>();
	protected final int BLOCK_ID_CAVE_AIR;
	protected final Set<Integer> BLOCK_ID_AIRS;
	protected final Set<Integer> BLOCK_ID_SIGNS;

	public ConfigDefault() {
		this.initBlockIds();

		this.BLOCK_ID_CAVE_AIR = this.getMaterialIds(Material.CAVE_AIR).iterator().next();
		this.BLOCK_ID_AIRS = this
				.convertMaterialsToSet(new Material[] { Material.AIR, Material.CAVE_AIR, Material.VOID_AIR });
		this.BLOCK_ID_SIGNS = this.convertMaterialsToSet(new Material[] { Material.ACACIA_SIGN, Material.BIRCH_SIGN,
				Material.DARK_OAK_SIGN, Material.JUNGLE_SIGN, Material.OAK_SIGN, Material.SPRUCE_SIGN,
				Material.ACACIA_WALL_SIGN, Material.BIRCH_WALL_SIGN, Material.DARK_OAK_WALL_SIGN,
				Material.JUNGLE_WALL_SIGN, Material.OAK_WALL_SIGN, Material.SPRUCE_WALL_SIGN });

		// Default World
		this.defaultProximityHiderBlockIds = this.convertMaterialsToIds(
				new Material[] { Material.DISPENSER, Material.SPAWNER, Material.CHEST, Material.TRAPPED_CHEST,
						Material.ENDER_CHEST, Material.SHULKER_BOX, Material.HOPPER, Material.CRAFTING_TABLE,
						Material.FURNACE, Material.ENCHANTING_TABLE, Material.ANVIL, Material.CHIPPED_ANVIL,
						Material.DAMAGED_ANVIL, Material.EMERALD_ORE, Material.DIAMOND_ORE, Material.REDSTONE_ORE });

		this.defaultDarknessBlockIds = this.convertMaterialsToIds(
				new Material[] { Material.SPAWNER, Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST });

		this.defaultMode1BlockId = this.getMaterialIds(Material.STONE).iterator().next();
		this.defaultProximityHiderSpecialBlockId = this.getMaterialIds(Material.STONE).iterator().next();

		// The End
		this.endWorldRandomBlockIds = this.convertMaterialsToIds(new Material[] { Material.BEDROCK, Material.OBSIDIAN,
				Material.END_STONE, Material.PURPUR_BLOCK, Material.END_STONE_BRICKS });

		this.endWorldObfuscateBlockIds = this.convertMaterialsToIds(new Material[] { Material.END_STONE });

		this.endWorldMode1BlockId = this.getMaterialIds(Material.END_STONE).iterator().next();
		this.endWorldRequiredObfuscateBlockIds = this.convertMaterialsToIds(new Material[] { Material.END_STONE });

		// Nether World
		this.netherWorldRandomBlockIds = this.convertMaterialsToIds(new Material[] { Material.GRAVEL,
				Material.NETHERRACK, Material.SOUL_SAND, Material.NETHER_BRICKS, Material.NETHER_QUARTZ_ORE });

		this.netherWorldObfuscateBlockIds = this
				.convertMaterialsToIds(new Material[] { Material.NETHERRACK, Material.NETHER_QUARTZ_ORE });

		this.netherWorldMode1BlockId = this.getMaterialIds(Material.NETHERRACK).iterator().next();

		this.netherWorldRequiredObfuscateBlockIds = this.convertMaterialsToIds(new Material[] { Material.NETHERRACK });

		// Normal World
		this.normalWorldRandomBlockIds = this.convertMaterialsToIds(new Material[] { Material.STONE,
				Material.COBBLESTONE, Material.OAK_PLANKS, Material.GOLD_ORE, Material.IRON_ORE, Material.COAL_ORE,
				Material.LAPIS_ORE, Material.TNT, Material.MOSSY_COBBLESTONE, Material.OBSIDIAN, Material.DIAMOND_ORE,
				Material.REDSTONE_ORE, Material.CLAY, Material.EMERALD_ORE });

		this.normalWorldObfuscateBlockIds = this.convertMaterialsToIds(new Material[] { Material.GOLD_ORE,
				Material.IRON_ORE, Material.COAL_ORE, Material.LAPIS_ORE, Material.CHEST, Material.TRAPPED_CHEST,
				Material.ENDER_CHEST, Material.DIAMOND_ORE, Material.ENDER_CHEST, Material.REDSTONE_ORE, Material.CLAY,
				Material.EMERALD_ORE, Material.SHULKER_BOX });

		this.normalWorldMode1BlockId = this.getMaterialIds(Material.STONE).iterator().next();

		this.normalWorldRequiredObfuscateBlockIds = this.convertMaterialsToIds(new Material[] { Material.STONE });
	}

	private void initBlockIds() {
		this.materialIds.clear();

		Block.REGISTRY_ID.iterator().forEachRemaining(blockData -> {
			Material material = CraftBlockData.fromData(blockData).getMaterial();

			if (material.isBlock()) {
				int materialId = Block.REGISTRY_ID.getId(blockData);

				Set<Integer> ids = this.materialIds.get(material);

				if (ids == null) {
					this.materialIds.put(material, ids = new HashSet<Integer>());
				}

				ids.add(materialId);
			}
		});
	}

	public Set<Integer> getMaterialIds(Material material) {
		return this.materialIds.get(material);
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