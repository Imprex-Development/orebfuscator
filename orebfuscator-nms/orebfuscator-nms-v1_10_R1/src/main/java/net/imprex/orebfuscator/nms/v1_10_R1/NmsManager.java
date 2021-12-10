package net.imprex.orebfuscator.nms.v1_10_R1;

import java.util.Iterator;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_10_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_10_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractBlockState;
import net.imprex.orebfuscator.nms.AbstractNmsManager;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.minecraft.server.v1_10_R1.Block;
import net.minecraft.server.v1_10_R1.BlockAir;
import net.minecraft.server.v1_10_R1.BlockPosition;
import net.minecraft.server.v1_10_R1.Chunk;
import net.minecraft.server.v1_10_R1.ChunkProviderServer;
import net.minecraft.server.v1_10_R1.EntityPlayer;
import net.minecraft.server.v1_10_R1.IBlockData;
import net.minecraft.server.v1_10_R1.MathHelper;
import net.minecraft.server.v1_10_R1.MinecraftKey;
import net.minecraft.server.v1_10_R1.PacketPlayOutBlockChange;
import net.minecraft.server.v1_10_R1.TileEntity;
import net.minecraft.server.v1_10_R1.WorldServer;

public class NmsManager extends AbstractNmsManager {

	private static WorldServer world(World world) {
		return ((CraftWorld) world).getHandle();
	}

	private static EntityPlayer player(Player player) {
		return ((CraftPlayer) player).getHandle();
	}

	private static boolean isChunkLoaded(WorldServer world, int chunkX, int chunkZ) {
		return world.getChunkProviderServer().isLoaded(chunkX, chunkZ);
	}

	private static IBlockData getBlockData(World world, int x, int y, int z, boolean loadChunk) {
		WorldServer worldServer = world(world);
		ChunkProviderServer chunkProviderServer = worldServer.getChunkProviderServer();

		if (isChunkLoaded(worldServer, x >> 4, z >> 4) || loadChunk) {
			// will load chunk if not loaded already
			Chunk chunk = chunkProviderServer.getOrLoadChunkAt(x >> 4, z >> 4);
			return chunk != null ? chunk.a(x, y, z) : null;
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

	public NmsManager(Config config) {
		super(config);

		for (Iterator<IBlockData> iterator = Block.REGISTRY_ID.iterator(); iterator.hasNext();) {
			IBlockData blockData = iterator.next();
			Material material = CraftMagicNumbers.getMaterial(blockData.getBlock());
			int blockId = getBlockId(blockData);
			this.registerMaterialId(material, blockId);
			Block block = blockData.getBlock();
			this.setBlockFlags(blockId, block instanceof BlockAir, material.isOccluding(), block.isTileEntity());
		}
	}

	@Override
	protected AbstractRegionFileCache<?> createRegionFileCache(CacheConfig cacheConfig) {
		return new RegionFileCache(cacheConfig);
	}

	@Override
	public int getBitsPerBlock() {
		// from DataPaletteBlock::b(int var0) L10
		return MathHelper.d(Block.REGISTRY_ID.a());
	}

	@Override
	public int getTotalBlockCount() {
		return Block.REGISTRY_ID.a();
	}

	@Override
	public Optional<Material> getMaterialByName(String name) {
		MinecraftKey minecraftKey = new MinecraftKey(name);
		if (Block.REGISTRY.d(minecraftKey)) {
			return Optional.ofNullable(CraftMagicNumbers.getMaterial(Block.REGISTRY.get(minecraftKey)));
		}
		return Optional.empty();
	}

	@Override
	public Optional<String> getNameByMaterial(Material material) {
		MinecraftKey key = Block.REGISTRY.b(CraftMagicNumbers.getBlock(material));
		if (key != null) {
			return Optional.of(key.toString());
		}
		return Optional.empty();
	}

	@Override
	public boolean isHoe(Material material) {
		switch (material) {
		case WOOD_HOE:
		case STONE_HOE:
		case IRON_HOE:
		case GOLD_HOE:
		case DIAMOND_HOE:
			return true;

		default:
			return false;
		}
	}

	@Override
	public ReadOnlyChunk getReadOnlyChunk(World world, int chunkX, int chunkZ) {
		ChunkProviderServer chunkProviderServer = world(world).getChunkProviderServer();
		Chunk chunk = chunkProviderServer.getOrLoadChunkAt(chunkX, chunkZ);
		return new ReadOnlyChunkWrapper(chunk);
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
	public boolean sendBlockChange(Player player, int x, int y, int z) {
		EntityPlayer entityPlayer = player(player);
		WorldServer world = entityPlayer.x();
		if (!isChunkLoaded(world, x >> 4, z >> 4)) {
			return false;
		}

		BlockPosition position = new BlockPosition(x, y, z);
		PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(world, position);
		entityPlayer.playerConnection.sendPacket(packet);
		updateTileEntity(entityPlayer, position, packet.block);

		return true;
	}

	private void updateTileEntity(EntityPlayer player, BlockPosition position, IBlockData blockData) {
		if (blockData.getBlock().isTileEntity()) {
			WorldServer worldServer = player.x();
			TileEntity tileEntity = worldServer.getTileEntity(position);
			if (tileEntity != null) {
				player.playerConnection.sendPacket(tileEntity.getUpdatePacket());
			}
		}
	}
}
