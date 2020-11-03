package net.imprex.orebfuscator.nms.v1_16_R3;

import java.util.BitSet;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R3.util.CraftMagicNumbers;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractBlockState;
import net.imprex.orebfuscator.nms.AbstractNmsManager;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.util.BlockPos;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Chunk;
import net.minecraft.server.v1_16_R3.ChunkProviderServer;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.MinecraftKey;
import net.minecraft.server.v1_16_R3.Packet;
import net.minecraft.server.v1_16_R3.PacketPlayOutBlockChange;
import net.minecraft.server.v1_16_R3.TileEntity;
import net.minecraft.server.v1_16_R3.WorldServer;

public class NmsManager extends AbstractNmsManager {

	private static WorldServer world(World world) {
		return ((CraftWorld) world).getHandle();
	}

	private static EntityPlayer player(Player player) {
		return ((CraftPlayer) player).getHandle();
	}

	private static boolean isChunkLoaded(WorldServer world, int chunkX, int chunkZ) {
		return world.isChunkLoaded(chunkX, chunkZ);
	}

	private static IBlockData getBlockData(World world, int x, int y, int z, boolean loadChunk) {
		WorldServer worldServer = world(world);
		ChunkProviderServer chunkProviderServer = worldServer.getChunkProvider();

		if (isChunkLoaded(worldServer, x >> 4, z >> 4) || loadChunk) {
			// will load chunk if not loaded already
			Chunk chunk = chunkProviderServer.getChunkAt(x >> 4, z >> 4, true);
			return chunk != null ? chunk.getType(new BlockPosition(x, y, z)) : null;
		}
		return null;
	}

	static int getBlockId(IBlockData blockData) {
		if (blockData == null) {
			return 0;
		} else {
			int id = Block.REGISTRY_ID.getId(blockData);
			return id == -1 ? 0 : id;
		}
	}

	private final int blockIdCaveAir;
	private final BitSet blockAir;

	public NmsManager(Config config) {
		super(config);

		for (IBlockData blockData : Block.REGISTRY_ID) {
			Material material = CraftBlockData.fromData(blockData).getMaterial();
			this.registerMaterialId(material, getBlockId(blockData));
		}

		this.blockIdCaveAir = this.getMaterialIds(Material.CAVE_AIR).iterator().next();
		this.blockAir = this.materialsToBitSet(Material.AIR, Material.CAVE_AIR, Material.VOID_AIR);
	}

	@Override
	protected AbstractRegionFileCache<?> createRegionFileCache(CacheConfig cacheConfig) {
		return new RegionFileCache(cacheConfig);
	}

	@Override
	public int getBitsPerBlock() {
		return MathHelper.e(Block.REGISTRY_ID.a());
	}

	@Override
	public int getMaterialSize() {
		return Block.REGISTRY_ID.a();
	}

	@Override
	public Optional<Material> getMaterialByName(String name) {
		Optional<Block> block = IRegistry.BLOCK.getOptional(new MinecraftKey(name));
		if (block.isPresent()) {
			return Optional.ofNullable(CraftMagicNumbers.getMaterial(block.get()));
		}
		return Optional.empty();
	}

	@Override
	public Optional<String> getNameByMaterial(Material material) {
		MinecraftKey key = IRegistry.BLOCK.getKey(CraftMagicNumbers.getBlock(material));
		if (key != null) {
			return Optional.of(key.toString());
		}
		return Optional.empty();
	}

	@Override
	public int getCaveAirBlockId() {
		return this.blockIdCaveAir;
	}

	@Override
	public boolean isHoe(Material material) {
		switch (material) {
		case WOODEN_HOE:
		case STONE_HOE:
		case IRON_HOE:
		case GOLDEN_HOE:
		case DIAMOND_HOE:
		case NETHERITE_HOE:
			return true;

		default:
			return false;
		}
	}

	@Override
	public boolean isAir(int blockId) {
		return this.blockAir.get(blockId);
	}

	@Override
	public boolean isTileEntity(int blockId) {
		return Block.getByCombinedId(blockId).getBlock().isTileEntity();
	}

	@Override
	public boolean canApplyPhysics(Material material) {
		switch (material) {
		case AIR:
		case CAVE_AIR:
		case VOID_AIR:
		case FIRE:
		case WATER:
		case LAVA:
			return true;

		default:
			return false;
		}
	}

	@Override
	public void updateBlockTileEntity(Player player, BlockPos blockCoord) {
		EntityPlayer entityPlayer = player(player);
		WorldServer world = entityPlayer.getWorldServer();

		TileEntity tileEntity = world.getTileEntity(new BlockPosition(blockCoord.x, blockCoord.y, blockCoord.z));
		if (tileEntity == null) {
			return;
		}

		Packet<?> packet = tileEntity.getUpdatePacket();
		if (packet != null) {
			entityPlayer.playerConnection.sendPacket(packet);
		}
	}

	@Override
	public int getBlockLightLevel(World world, int x, int y, int z) {
		return world(world).getLightLevel(new BlockPosition(x, y, z));
	}

	@Override
	public AbstractBlockState<?> getBlockState(World world, int x, int y, int z) {
		IBlockData blockData = getBlockData(world, x, y, z, false);
		return blockData != null ? new BlockState(x, y, z, world, blockData) : null;
	}

	@Override
	public int loadChunkAndGetBlockId(World world, int x, int y, int z) {
		IBlockData blockData = getBlockData(world, x, y, z, true);
		return blockData != null ? getBlockId(blockData) : -1;
	}

	@Override
	public boolean sendBlockChange(Player player, BlockPos blockCoord) {
		WorldServer world = world(player.getWorld());
		if (!isChunkLoaded(world, blockCoord.x >> 4, blockCoord.z >> 4)) {
			return false;
		}

		BlockPosition position = new BlockPosition(blockCoord.x, blockCoord.y, blockCoord.z);
		PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(world, position);
		player(player).playerConnection.sendPacket(packet);

		return true;
	}
}
