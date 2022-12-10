package net.imprex.orebfuscator.nms.v1_16_R1;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractBlockState;
import net.imprex.orebfuscator.nms.AbstractNmsManager;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.minecraft.server.v1_16_R1.Block;
import net.minecraft.server.v1_16_R1.BlockPosition;
import net.minecraft.server.v1_16_R1.Chunk;
import net.minecraft.server.v1_16_R1.ChunkProviderServer;
import net.minecraft.server.v1_16_R1.EntityPlayer;
import net.minecraft.server.v1_16_R1.IBlockData;
import net.minecraft.server.v1_16_R1.IRegistry;
import net.minecraft.server.v1_16_R1.MathHelper;
import net.minecraft.server.v1_16_R1.MinecraftKey;
import net.minecraft.server.v1_16_R1.PacketPlayOutBlockChange;
import net.minecraft.server.v1_16_R1.TileEntity;
import net.minecraft.server.v1_16_R1.WorldServer;

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

	public NmsManager(Config config) {
		super(config);

		for (IBlockData blockData : Block.REGISTRY_ID) {
			Material material = CraftBlockData.fromData(blockData).getMaterial();
			int blockId = getBlockId(blockData);
			this.registerMaterialId(material, blockId);
			/**
			 * l -> for barrier/slime_block/spawner/leaves
			 * isOccluding -> for every other block
			 */
			this.setBlockFlags(blockId, blockData.isAir(), material.isOccluding() && blockData.l()/*canOcclude*/, blockData.getBlock().isTileEntity());
		}
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
	public int getTotalBlockCount() {
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
	public ReadOnlyChunk getReadOnlyChunk(World world, int chunkX, int chunkZ) {
		ChunkProviderServer chunkProviderServer = world(world).getChunkProvider();
		Chunk chunk = chunkProviderServer.getChunkAt(chunkX, chunkZ, true);
		return new ReadOnlyChunkWrapper(chunk);
	}

	@Override
	public AbstractBlockState<?> getBlockState(World world, int x, int y, int z) {
		IBlockData blockData = getBlockData(world, x, y, z, false);
		return blockData != null ? new BlockState(x, y, z, world, blockData) : null;
	}

	@Override
	public boolean sendBlockChange(Player player, int x, int y, int z) {
		EntityPlayer entityPlayer = player(player);
		WorldServer world = entityPlayer.getWorldServer();
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
			WorldServer worldServer = player.getWorldServer();
			TileEntity tileEntity = worldServer.getTileEntity(position);
			if (tileEntity != null) {
				player.playerConnection.sendPacket(tileEntity.getUpdatePacket());
			}
		}
	}
}
