package net.imprex.orebfuscator.nms.v1_13_R2;

import java.util.Iterator;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractBlockState;
import net.imprex.orebfuscator.nms.AbstractNmsManager;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.util.BlockPos;
import net.minecraft.server.v1_13_R2.Block;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.Chunk;
import net.minecraft.server.v1_13_R2.ChunkProviderServer;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.IRegistry;
import net.minecraft.server.v1_13_R2.MathHelper;
import net.minecraft.server.v1_13_R2.MinecraftKey;
import net.minecraft.server.v1_13_R2.PacketPlayOutBlockChange;
import net.minecraft.server.v1_13_R2.TileEntity;
import net.minecraft.server.v1_13_R2.WorldServer;

public class NmsManager extends AbstractNmsManager {

	private static WorldServer world(World world) {
		return ((CraftWorld) world).getHandle();
	}

	private static EntityPlayer player(Player player) {
		return ((CraftPlayer) player).getHandle();
	}

	private static boolean isChunkLoaded(WorldServer world, int chunkX, int chunkZ) {
		return world.getChunkProvider().isLoaded(chunkX, chunkZ);
	}

	private static IBlockData getBlockData(World world, int x, int y, int z, boolean loadChunk) {
		WorldServer worldServer = world(world);
		ChunkProviderServer chunkProviderServer = worldServer.getChunkProvider();

		if (isChunkLoaded(worldServer, x >> 4, z >> 4) || loadChunk) {
			// will load chunk if not loaded already
			Chunk chunk = chunkProviderServer.getChunkAt(x >> 4, z >> 4, true, false);
			return chunk != null ? chunk.getBlockData(x, y, z) : null;
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

	public NmsManager(Config config) {
		super(config);

		for (Iterator<IBlockData> iterator = Block.REGISTRY_ID.iterator(); iterator.hasNext();) {
			IBlockData blockData = iterator.next();
			Material material = CraftBlockData.fromData(blockData).getMaterial();
			int blockId = getBlockId(blockData);
			this.registerMaterialId(material, blockId);
			this.setBlockFlags(blockId, blockData.isAir(), blockData.getBlock().isTileEntity());
		}

		this.blockIdCaveAir = this.getMaterialIds(Material.CAVE_AIR).iterator().next();
	}

	@Override
	protected AbstractRegionFileCache<?> createRegionFileCache(CacheConfig cacheConfig) {
		return new RegionFileCache(cacheConfig);
	}

	@Override
	public int getBitsPerBlock() {
		return MathHelper.d(Block.REGISTRY_ID.a());
	}

	@Override
	public int getTotalBlockCount() {
		return Block.REGISTRY_ID.a();
	}

	@Override
	public Optional<Material> getMaterialByName(String name) {
		Block block = IRegistry.BLOCK.get(new MinecraftKey(name));
		if (block != null) {
			return Optional.ofNullable(CraftMagicNumbers.getMaterial(block));
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
			return true;

		default:
			return false;
		}
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
	public boolean sendBlockChange(Player bukkitPlayer, BlockPos blockCoord) {
		EntityPlayer player = player(bukkitPlayer);
		WorldServer world = player.getWorldServer();
		if (!isChunkLoaded(world, blockCoord.x >> 4, blockCoord.z >> 4)) {
			return false;
		}

		BlockPosition position = new BlockPosition(blockCoord.x, blockCoord.y, blockCoord.z);
		PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(world, position);
		player.playerConnection.sendPacket(packet);
		updateTileEntity(player, position, packet.block);

		return true;
	}

	private void updateTileEntity(EntityPlayer player, BlockPosition position, IBlockData blockData) {
		if (blockData.getBlock().isTileEntity()) {
			WorldServer worldServer = player.getWorldServer();
			TileEntity tileEntity = worldServer.getTileEntity(position);
			if (tileEntity != null) {
				player.playerConnection.sendPacket(tileEntity.getUpdatePacket());
			}
		}
	}
}
