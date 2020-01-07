/**
 * @author lishid
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.nms.v1_13_R2;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftChatMessage;
import org.bukkit.entity.Player;

import com.lishid.orebfuscator.nms.IBlockInfo;
import com.lishid.orebfuscator.nms.IChunkCache;
import com.lishid.orebfuscator.nms.INBT;
import com.lishid.orebfuscator.nms.INmsManager;
import com.lishid.orebfuscator.types.ConfigDefaults;

import net.imprex.orebfuscator.util.BlockCoords;
import net.minecraft.server.v1_13_R2.Block;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.Chunk;
import net.minecraft.server.v1_13_R2.ChunkProviderServer;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.IChatBaseComponent;
import net.minecraft.server.v1_13_R2.Packet;
import net.minecraft.server.v1_13_R2.TileEntity;
import net.minecraft.server.v1_13_R2.WorldServer;

public class NmsManager implements INmsManager {
	private static final int BITS_PER_BLOCK = 14;

	private int BLOCK_ID_CAVE_AIR;
	private Set<Integer> BLOCK_ID_AIRS;
	private Set<Integer> BLOCK_ID_SIGNS;

	private ConfigDefaults configDefaults;
	private int maxLoadedCacheFiles;
	private Material[] extraTransparentBlocks;
	private HashMap<Material, Set<Integer>> materialIds;

	public NmsManager() {
		initBlockIds();

		this.BLOCK_ID_CAVE_AIR = getMaterialIds(Material.CAVE_AIR).iterator().next();
		this.BLOCK_ID_AIRS = convertMaterialsToSet(new Material[] { Material.AIR, Material.CAVE_AIR, Material.VOID_AIR });
		this.BLOCK_ID_SIGNS = convertMaterialsToSet(new Material[] { Material.SIGN, Material.WALL_SIGN });

		this.configDefaults = new ConfigDefaults();

		// Default World

		this.configDefaults.defaultProximityHiderBlockIds = convertMaterialsToIds(new Material[] {
				Material.DISPENSER,
				Material.SPAWNER,
				Material.CHEST,
				Material.HOPPER,
				Material.CRAFTING_TABLE,
				Material.FURNACE,
				Material.ENCHANTING_TABLE,
				Material.EMERALD_ORE,
				Material.ENDER_CHEST,
				Material.ANVIL,
				Material.CHIPPED_ANVIL,
				Material.DAMAGED_ANVIL,
				Material.TRAPPED_CHEST,
				Material.DIAMOND_ORE
		});

		this.configDefaults.defaultDarknessBlockIds = convertMaterialsToIds(new Material[] {
				Material.SPAWNER,
				Material.CHEST
		});

		this.configDefaults.defaultMode1BlockId = getMaterialIds(Material.STONE).iterator().next();
		this.configDefaults.defaultProximityHiderSpecialBlockId = getMaterialIds(Material.STONE).iterator().next();

		// The End

		this.configDefaults.endWorldRandomBlockIds = convertMaterialsToIds(new Material[] {
				Material.BEDROCK,
				Material.OBSIDIAN,
				Material.END_STONE,
				Material.PURPUR_BLOCK,
				Material.END_STONE_BRICKS
		});

		this.configDefaults.endWorldObfuscateBlockIds = convertMaterialsToIds(new Material[] {
				Material.END_STONE
		});

		this.configDefaults.endWorldMode1BlockId = getMaterialIds(Material.END_STONE).iterator().next();
		this.configDefaults.endWorldRequiredObfuscateBlockIds = convertMaterialsToIds(new Material[] { Material.END_STONE });

		// Nether World

		this.configDefaults.netherWorldRandomBlockIds = convertMaterialsToIds(new Material[] {
				Material.GRAVEL,
				Material.NETHERRACK,
				Material.SOUL_SAND,
				Material.NETHER_BRICKS,
				Material.NETHER_QUARTZ_ORE
		});

		this.configDefaults.netherWorldObfuscateBlockIds = convertMaterialsToIds(new Material[] {
				Material.NETHERRACK,
				Material.NETHER_QUARTZ_ORE
		});

		this.configDefaults.netherWorldMode1BlockId = getMaterialIds(Material.NETHERRACK).iterator().next();

		this.configDefaults.netherWorldRequiredObfuscateBlockIds = convertMaterialsToIds(new Material[] {
				Material.NETHERRACK
		});

		// Normal World

		this.configDefaults.normalWorldRandomBlockIds = convertMaterialsToIds(new Material[] {
				Material.STONE,
				Material.COBBLESTONE,
				Material.OAK_PLANKS,
				Material.GOLD_ORE,
				Material.IRON_ORE,
				Material.COAL_ORE,
				Material.LAPIS_ORE,
				Material.TNT,
				Material.MOSSY_COBBLESTONE,
				Material.OBSIDIAN,
				Material.DIAMOND_ORE,
				Material.REDSTONE_ORE,
				Material.CLAY,
				Material.EMERALD_ORE
		});

		this.configDefaults.normalWorldObfuscateBlockIds = convertMaterialsToIds(new Material[] {
				Material.GOLD_ORE,
				Material.IRON_ORE,
				Material.COAL_ORE,
				Material.LAPIS_ORE,
				Material.CHEST,
				Material.DIAMOND_ORE,
				Material.ENDER_CHEST,
				Material.REDSTONE_ORE,
				Material.CLAY,
				Material.EMERALD_ORE
		});

		this.configDefaults.normalWorldMode1BlockId = getMaterialIds(Material.STONE).iterator().next();

		this.configDefaults.normalWorldRequiredObfuscateBlockIds = convertMaterialsToIds(new Material[] {
				Material.STONE
		});

		// Extra transparent blocks

		this.extraTransparentBlocks = new Material[] {
				Material.ACACIA_DOOR,
				Material.ACACIA_FENCE,
				Material.ACACIA_FENCE_GATE,
				Material.ACACIA_LEAVES,
				Material.ACACIA_PRESSURE_PLATE,
				Material.ACACIA_SLAB,
				Material.ACACIA_STAIRS,
				Material.ACACIA_TRAPDOOR,
				Material.ANVIL,
				Material.BEACON,
				Material.BIRCH_DOOR,
				Material.BIRCH_FENCE,
				Material.BIRCH_FENCE_GATE,
				Material.BIRCH_LEAVES,
				Material.BIRCH_PRESSURE_PLATE,
				Material.BIRCH_SLAB,
				Material.BIRCH_STAIRS,
				Material.BIRCH_TRAPDOOR,
				Material.BLACK_BANNER,
				Material.BLACK_BED,
				Material.BLACK_STAINED_GLASS,
				Material.BLACK_STAINED_GLASS_PANE,
				Material.BLACK_WALL_BANNER,
				Material.BLUE_BANNER,
				Material.BLUE_BED,
				Material.BLUE_ICE,
				Material.BLUE_STAINED_GLASS,
				Material.BLUE_STAINED_GLASS_PANE,
				Material.BLUE_WALL_BANNER,
				Material.BREWING_STAND,
				Material.BRICK_SLAB,
				Material.BRICK_STAIRS,
				Material.BRAIN_CORAL,
				Material.BRAIN_CORAL_FAN,
				Material.BRAIN_CORAL_WALL_FAN,
				Material.BROWN_BANNER,
				Material.BROWN_BED,
				Material.BROWN_STAINED_GLASS,
				Material.BROWN_STAINED_GLASS_PANE,
				Material.BROWN_WALL_BANNER,
				Material.BUBBLE_COLUMN,
				Material.BUBBLE_CORAL,
				Material.BUBBLE_CORAL_FAN,
				Material.BUBBLE_CORAL_WALL_FAN,
				Material.CACTUS,
				Material.CAKE,
				Material.CAULDRON,
				Material.CHIPPED_ANVIL,
				Material.COBBLESTONE_SLAB,
				Material.COBBLESTONE_STAIRS,
				Material.COBBLESTONE_WALL,
				Material.COBWEB,
				Material.CONDUIT,
				Material.CYAN_BANNER,
				Material.CYAN_BED,
				Material.CYAN_STAINED_GLASS,
				Material.CYAN_STAINED_GLASS_PANE,
				Material.CYAN_WALL_BANNER,
				Material.DAMAGED_ANVIL,
				Material.DARK_OAK_DOOR,
				Material.DARK_OAK_FENCE,
				Material.DARK_OAK_FENCE_GATE,
				Material.DARK_OAK_LEAVES,
				Material.DARK_OAK_PRESSURE_PLATE,
				Material.DARK_OAK_SLAB,
				Material.DARK_OAK_STAIRS,
				Material.DARK_OAK_TRAPDOOR,
				Material.DARK_PRISMARINE_SLAB,
				Material.DARK_PRISMARINE_STAIRS,
				Material.DAYLIGHT_DETECTOR,
				Material.DEAD_BRAIN_CORAL,
				Material.DEAD_BRAIN_CORAL_FAN,
				Material.DEAD_BRAIN_CORAL_WALL_FAN,
				Material.DEAD_BUBBLE_CORAL,
				Material.DEAD_BUBBLE_CORAL_FAN,
				Material.DEAD_BUBBLE_CORAL_WALL_FAN,
				Material.DEAD_FIRE_CORAL,
				Material.DEAD_FIRE_CORAL_FAN,
				Material.DEAD_FIRE_CORAL_WALL_FAN,
				Material.DEAD_HORN_CORAL,
				Material.DEAD_HORN_CORAL_FAN,
				Material.DEAD_HORN_CORAL_WALL_FAN,
				Material.DEAD_TUBE_CORAL,
				Material.DEAD_TUBE_CORAL_FAN,
				Material.DEAD_TUBE_CORAL_WALL_FAN,
				Material.DRAGON_EGG,
				Material.FARMLAND,
				Material.FIRE_CORAL,
				Material.FIRE_CORAL_FAN,
				Material.FIRE_CORAL_WALL_FAN,
				Material.FROSTED_ICE,
				Material.GLASS,
				Material.GLASS_PANE,
				Material.GRAY_BANNER,
				Material.GRAY_BED,
				Material.GRAY_STAINED_GLASS,
				Material.GRAY_STAINED_GLASS_PANE,
				Material.GRAY_WALL_BANNER,
				Material.GREEN_BANNER,
				Material.GREEN_BED,
				Material.GREEN_STAINED_GLASS,
				Material.GREEN_STAINED_GLASS_PANE,
				Material.GREEN_WALL_BANNER,
				Material.HEAVY_WEIGHTED_PRESSURE_PLATE,
				Material.HOPPER,
				Material.HORN_CORAL,
				Material.HORN_CORAL_FAN,
				Material.HORN_CORAL_WALL_FAN,
				Material.ICE,
				Material.IRON_BARS,
				Material.IRON_DOOR,
				Material.IRON_TRAPDOOR,
				Material.JUNGLE_DOOR,
				Material.JUNGLE_FENCE,
				Material.JUNGLE_FENCE_GATE,
				Material.JUNGLE_LEAVES,
				Material.JUNGLE_PRESSURE_PLATE,
				Material.JUNGLE_SLAB,
				Material.JUNGLE_STAIRS,
				Material.JUNGLE_TRAPDOOR,
				Material.KELP,
				Material.KELP_PLANT,
				Material.LIGHT_BLUE_BANNER,
				Material.LIGHT_BLUE_BED,
				Material.LIGHT_BLUE_STAINED_GLASS,
				Material.LIGHT_BLUE_STAINED_GLASS_PANE,
				Material.LIGHT_BLUE_WALL_BANNER,
				Material.LIGHT_GRAY_BANNER,
				Material.LIGHT_GRAY_BED,
				Material.LIGHT_GRAY_STAINED_GLASS,
				Material.LIGHT_GRAY_STAINED_GLASS_PANE,
				Material.LIGHT_GRAY_WALL_BANNER,
				Material.LIGHT_WEIGHTED_PRESSURE_PLATE,
				Material.LIME_BANNER,
				Material.LIME_BED,
				Material.LIME_STAINED_GLASS,
				Material.LIME_STAINED_GLASS_PANE,
				Material.LIME_WALL_BANNER,
				Material.MAGENTA_BANNER,
				Material.MAGENTA_BED,
				Material.MAGENTA_STAINED_GLASS,
				Material.MAGENTA_STAINED_GLASS_PANE,
				Material.MAGENTA_WALL_BANNER,
				Material.MOSSY_COBBLESTONE_WALL,
				Material.MOVING_PISTON,
				Material.NETHER_BRICK_FENCE,
				Material.NETHER_BRICK_SLAB,
				Material.NETHER_BRICK_STAIRS,
				Material.OAK_DOOR,
				Material.OAK_FENCE,
				Material.OAK_FENCE_GATE,
				Material.OAK_LEAVES,
				Material.OAK_PRESSURE_PLATE,
				Material.OAK_SLAB,
				Material.OAK_STAIRS,
				Material.OAK_TRAPDOOR,
				Material.ORANGE_BANNER,
				Material.ORANGE_BED,
				Material.ORANGE_STAINED_GLASS,
				Material.ORANGE_STAINED_GLASS_PANE,
				Material.ORANGE_WALL_BANNER,
				Material.PACKED_ICE,
				Material.PETRIFIED_OAK_SLAB,
				Material.PINK_BANNER,
				Material.PINK_BED,
				Material.PINK_STAINED_GLASS,
				Material.PINK_STAINED_GLASS_PANE,
				Material.PINK_WALL_BANNER,
				Material.PISTON,
				Material.PISTON_HEAD,
				Material.PRISMARINE_BRICK_SLAB,
				Material.PRISMARINE_BRICK_STAIRS,
				Material.PRISMARINE_SLAB,
				Material.PRISMARINE_STAIRS,
				Material.PURPLE_BANNER,
				Material.PURPLE_BED,
				Material.PURPLE_STAINED_GLASS,
				Material.PURPLE_STAINED_GLASS_PANE,
				Material.PURPLE_WALL_BANNER,
				Material.PURPUR_SLAB,
				Material.PURPUR_STAIRS,
				Material.QUARTZ_SLAB,
				Material.QUARTZ_STAIRS,
				Material.RED_BANNER,
				Material.RED_BED,
				Material.RED_SANDSTONE_SLAB,
				Material.RED_SANDSTONE_STAIRS,
				Material.RED_STAINED_GLASS,
				Material.RED_STAINED_GLASS_PANE,
				Material.RED_WALL_BANNER,
				Material.SANDSTONE_SLAB,
				Material.SANDSTONE_STAIRS,
				Material.SEAGRASS,
				Material.SEA_PICKLE,
				Material.SIGN,
				Material.SLIME_BLOCK,
				Material.SPAWNER,
				Material.SPRUCE_DOOR,
				Material.SPRUCE_FENCE,
				Material.SPRUCE_FENCE_GATE,
				Material.SPRUCE_LEAVES,
				Material.SPRUCE_PRESSURE_PLATE,
				Material.SPRUCE_SLAB,
				Material.SPRUCE_STAIRS,
				Material.SPRUCE_TRAPDOOR,
				Material.STICKY_PISTON,
				Material.STONE_BRICK_SLAB,
				Material.STONE_BRICK_STAIRS,
				Material.STONE_PRESSURE_PLATE,
				Material.STONE_SLAB,
				Material.TALL_SEAGRASS,
				Material.TUBE_CORAL,
				Material.TUBE_CORAL_FAN,
				Material.TUBE_CORAL_WALL_FAN,
				Material.TURTLE_EGG,
				Material.WALL_SIGN,
				Material.WATER,
				Material.WHITE_BANNER,
				Material.WHITE_BED,
				Material.WHITE_STAINED_GLASS,
				Material.WHITE_STAINED_GLASS_PANE,
				Material.WHITE_WALL_BANNER,
				Material.YELLOW_BANNER,
				Material.YELLOW_BED,
				Material.YELLOW_STAINED_GLASS,
				Material.YELLOW_STAINED_GLASS_PANE,
				Material.YELLOW_WALL_BANNER
		};
	}

	private void initBlockIds() {
		this.materialIds = new HashMap<>();

		Block.REGISTRY_ID.iterator().forEachRemaining(blockData -> {
			Material material = CraftBlockData.fromData(blockData).getMaterial();

			if(material.isBlock()) {
				int materialId = Block.REGISTRY_ID.getId(blockData);

				Set<Integer> ids = this.materialIds.get(material);

				if (ids == null) {
					this.materialIds.put(material, ids = new HashSet<>());
				}

				ids.add(materialId);
			}
		});
	}

	public Material[] getExtraTransparentBlocks() {
		return this.extraTransparentBlocks;
	}

	public ConfigDefaults getConfigDefaults() {
		return this.configDefaults;
	}

	public void setMaxLoadedCacheFiles(int value) {
		this.maxLoadedCacheFiles = value;
	}

	public INBT createNBT() {
		return new NBT();
	}

	public IChunkCache createChunkCache() {
		return new ChunkCache(this.maxLoadedCacheFiles);
	}

	public void updateBlockTileEntity(BlockCoords blockCoord, Player player) {
		CraftWorld world = (CraftWorld)player.getWorld();
		// 1.13.2 has made this quite a bit different in later builds.
		TileEntity tileEntity = null;
		try {
			Method getTileEntityAt = world.getClass().getMethod("getTileEntityAt", int.class, int.class, int.class);
			tileEntity = (TileEntity) getTileEntityAt.invoke(world, blockCoord.x, blockCoord.y, blockCoord.z);
		} catch (NoSuchMethodException nsme) {
			tileEntity = world.getHandle().getTileEntity(new BlockPosition(blockCoord.x, blockCoord.y, blockCoord.z));
		} catch (Exception e) {
			return;
		}

		if (tileEntity == null) {
			return;
		}

		Packet<?> packet = tileEntity.getUpdatePacket();

		if (packet != null) {
			CraftPlayer player2 = (CraftPlayer)player;
			player2.getHandle().playerConnection.sendPacket(packet);
		}
	}

	public void notifyBlockChange(World world, IBlockInfo blockInfo) {
		BlockPosition blockPosition = new BlockPosition(blockInfo.getX(), blockInfo.getY(), blockInfo.getZ());
		IBlockData blockData = ((BlockInfo)blockInfo).getBlockData();

		((CraftWorld)world).getHandle().notify(blockPosition, blockData, blockData, 0);
	}

	public int getBlockLightLevel(World world, int x, int y, int z) {
		return ((CraftWorld)world).getHandle().getLightLevel(new BlockPosition(x, y, z));
	}

	public IBlockInfo getBlockInfo(World world, int x, int y, int z) {
		IBlockData blockData = getBlockData(world, x, y, z, false);

		return blockData != null
				? new BlockInfo(x, y, z, blockData)
				: null;
	}

	public int loadChunkAndGetBlockId(World world, int x, int y, int z) {
		IBlockData blockData = getBlockData(world, x, y, z, true);
		return blockData != null ? Block.getCombinedId(blockData): -1;
	}

	public String getTextFromChatComponent(String json) {
		IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a(json);
		return CraftChatMessage.fromComponent(component);
	}

	public boolean isHoe(Material item) {
		return item == Material.WOODEN_HOE
				|| item == Material.IRON_HOE
				|| item == Material.GOLDEN_HOE
				|| item == Material.DIAMOND_HOE;
	}

	public boolean isSign(int combinedBlockId) {
		return BLOCK_ID_SIGNS.contains(combinedBlockId);
	}

	public boolean isAir(int combinedBlockId) {
		return BLOCK_ID_AIRS.contains(combinedBlockId);
	}

	public boolean isTileEntity(int combinedBlockId) {
		return Block.getByCombinedId(combinedBlockId).getBlock().isTileEntity();
	}

	public int getCaveAirBlockId() {
		return BLOCK_ID_CAVE_AIR;
	}

	public int getBitsPerBlock() {
		return BITS_PER_BLOCK;
	}

	public boolean canApplyPhysics(Material blockMaterial) {
		return blockMaterial == Material.AIR
				|| blockMaterial == Material.CAVE_AIR
				|| blockMaterial == Material.VOID_AIR
				|| blockMaterial == Material.FIRE
				|| blockMaterial == Material.WATER
				|| blockMaterial == Material.LAVA;
	}

	public Set<Integer> getMaterialIds(Material material) {
		return this.materialIds.get(material);
	}

	public boolean sendBlockChange(Player player, Location blockLocation) {
		IBlockData blockData = getBlockData(blockLocation.getWorld(), blockLocation.getBlockX(), blockLocation.getBlockY(), blockLocation.getBlockZ(), false);

		if(blockData == null) return false;

		CraftBlockData craftBlockData = CraftBlockData.fromData(blockData);

		player.sendBlockChange(blockLocation, craftBlockData);

		return true;
	}

	private static IBlockData getBlockData(World world, int x, int y, int z, boolean loadChunk) {
		int chunkX = x >> 4;
		int chunkZ = z >> 4;

		WorldServer worldServer = ((CraftWorld)world).getHandle();
		// like in ChunkCache, NMS change without R increment.
		ChunkProviderServer chunkProviderServer = null;
		try {
			Method getChunkProviderServer = worldServer.getClass().getDeclaredMethod("getChunkProviderServer");
			chunkProviderServer = (ChunkProviderServer) getChunkProviderServer.invoke(worldServer);
		} catch (NoSuchMethodException nmfe) {
			try {
				Method getChunkProvider = worldServer.getClass().getDeclaredMethod("getChunkProvider");
				chunkProviderServer = (ChunkProviderServer) getChunkProvider.invoke(worldServer);
			} catch (NoSuchMethodException nsme) {
				return null; // oops
			} catch (Exception e) {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
		if(!loadChunk && !chunkProviderServer.isLoaded(chunkX, chunkZ)) return null;

		Chunk chunk = chunkProviderServer.getChunkAt(chunkX, chunkZ, true, true);

		return chunk != null ? chunk.getBlockData(x, y, z) : null;
	}

	private Set<Integer> convertMaterialsToSet(Material[] materials) {
		Set<Integer> ids = new HashSet<>();

		for(Material material : materials) {
			ids.addAll(getMaterialIds(material));
		}

		return ids;
	}

	private int[] convertMaterialsToIds(Material[] materials) {
		Set<Integer> ids = convertMaterialsToSet(materials);

		int[] result = new int[ids.size()];
		int index = 0;

		for(int id : ids) {
			result[index++] = id;
		}

		return result;
	}

	@Override
	public void sendMultiBlockChange(Player player, int chunkX, int chunkZ, Location... locations)
			throws IllegalAccessException, InvocationTargetException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean hasLightArray() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasBlockCount() {
		// TODO Auto-generated method stub
		return false;
	}
}