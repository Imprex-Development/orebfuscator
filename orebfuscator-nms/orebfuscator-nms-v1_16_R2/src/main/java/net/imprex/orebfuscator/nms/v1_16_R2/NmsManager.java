package net.imprex.orebfuscator.nms.v1_16_R2;

import java.util.Map.Entry;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.shorts.ShortArraySet;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.shorts.ShortSet;
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_16_R2.util.CraftMagicNumbers;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractBlockState;
import net.imprex.orebfuscator.nms.AbstractNmsManager;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.util.BlockPos;
import net.minecraft.server.v1_16_R2.Block;
import net.minecraft.server.v1_16_R2.BlockPosition;
import net.minecraft.server.v1_16_R2.Chunk;
import net.minecraft.server.v1_16_R2.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R2.ChunkProviderServer;
import net.minecraft.server.v1_16_R2.ChunkSection;
import net.minecraft.server.v1_16_R2.EntityPlayer;
import net.minecraft.server.v1_16_R2.IBlockData;
import net.minecraft.server.v1_16_R2.IRegistry;
import net.minecraft.server.v1_16_R2.MathHelper;
import net.minecraft.server.v1_16_R2.MinecraftKey;
import net.minecraft.server.v1_16_R2.Packet;
import net.minecraft.server.v1_16_R2.PacketPlayOutBlockChange;
import net.minecraft.server.v1_16_R2.PacketPlayOutMultiBlockChange;
import net.minecraft.server.v1_16_R2.SectionPosition;
import net.minecraft.server.v1_16_R2.TileEntity;
import net.minecraft.server.v1_16_R2.WorldServer;

public class NmsManager extends AbstractNmsManager {

	private static EntityPlayer player(Player player) {
		return ((CraftPlayer) player).getHandle();
	}

	private static void sendPacket(EntityPlayer player, Packet<?> packet) {
		if (packet != null) {
			player.playerConnection.sendPacket(packet);
		}
	}

	private static WorldServer world(World world) {
		return ((CraftWorld) world).getHandle();
	}

	private static boolean isChunkLoaded(WorldServer world, int chunkX, int chunkZ) {
		return world.isChunkLoaded(chunkX, chunkZ);
	}

	private static Chunk getChunk(WorldServer world, int chunkX, int chunkZ, boolean loadChunk) {
		ChunkProviderServer chunkProviderServer = world.getChunkProvider();

		if (isChunkLoaded(world, chunkX, chunkZ) || loadChunk) {
			// will load chunk if not loaded already
			return chunkProviderServer.getChunkAt(chunkX, chunkZ, true);
		}
		return null;
	}

	private static IBlockData getBlockData(World world, int x, int y, int z, boolean loadChunk) {
		Chunk chunk = getChunk(world(world), x >> 4, z >> 4, true);
		return chunk != null ? chunk.getType(new BlockPosition(x, y, z)) : null;
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

		for (IBlockData blockData : Block.REGISTRY_ID) {
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

	public void sendBlockUpdates(Player bukkitPlayer, BlockPos... positions) {
		EntityPlayer player = player(bukkitPlayer);
		boolean clientLightUpdate = positions.length >= 64;

		Long2ObjectMap<ShortSet[]> chunkMap = groupBlockUpdates(positions);
		for (Entry<Long, ShortSet[]> entry : chunkMap.long2ObjectEntrySet()) {
			long chunkPosition = entry.getKey();
			int chunkX = (int) chunkPosition;
			int chunkZ = (int) chunkPosition >> 32;

			WorldServer world = player.getWorldServer();
			Chunk chunk = getChunk(world, chunkX, chunkZ, false);
			if (chunk != null) {
				processChunk(player, chunk, entry.getValue(), clientLightUpdate);
			}
		}
	}

	private Long2ObjectMap<ShortSet[]> groupBlockUpdates(BlockPos[] positions) {
		Long2ObjectMap<ShortSet[]> map = new Long2ObjectOpenHashMap<>();
		for (BlockPos pos : positions) {
			int chunkY = pos.y >> 4;
			if (chunkY > 15) {
				continue;
			}

			long chunkPosition = ChunkCoordIntPair.pair(pos.x >> 4, pos.z >> 4);

			ShortSet[] blocks = map.computeIfAbsent(chunkPosition, key -> new ShortSet[16]);
			if (blocks[chunkY] == null) {
				blocks[chunkY] = new ShortArraySet();
			}
			blocks[chunkY].add(toLocalPosition(pos));
		}
		return map;
	}

	public static short toLocalPosition(BlockPos pos) {
		int x = pos.x & 15;
		int y = pos.y & 15;
		int z = pos.z & 15;
		return (short) (x << 8 | z << 4 | y << 0);
	}

	private void processChunk(EntityPlayer player, Chunk chunk, ShortSet[] chunkBlocks, boolean clientLightUpdate) {
		for (int y = 0; y < chunkBlocks.length; y++) {
			ShortSet sectionBlocks = chunkBlocks[y];
			if (sectionBlocks != null) {
				SectionPosition sectionPosition = SectionPosition.a(chunk.getPos(), y);
				if (sectionBlocks.size() == 1) {
					processSingleBlock(player, chunk, sectionPosition.g(sectionBlocks.iterator().nextShort()));
				} else {
					processMultiBlock(player, chunk, sectionPosition, sectionBlocks, clientLightUpdate);
				}
			}
		}
	}

	private void processSingleBlock(EntityPlayer player, Chunk chunk, BlockPosition position) {
		IBlockData blockData = chunk.getType(position);
		sendPacket(player, new PacketPlayOutBlockChange(position, blockData));
		updateTileEntity(player, position, blockData);
	}

	private void processMultiBlock(EntityPlayer player, Chunk chunk, SectionPosition sectionPosition,
			ShortSet sectionBlocks, boolean clientLightUpdate) {
		ChunkSection chunkSection = chunk.getSections()[sectionPosition.getY()];
		PacketPlayOutMultiBlockChange packet = new PacketPlayOutMultiBlockChange(sectionPosition, sectionBlocks,
				chunkSection, clientLightUpdate);
		sendPacket(player, packet);
		packet.a((position, blockData) -> {
			updateTileEntity(player, position, blockData);
		});
	}

	private void updateTileEntity(EntityPlayer player, BlockPosition position, IBlockData blockData) {
		if (blockData.getBlock().isTileEntity()) {
			WorldServer worldServer = player.getWorldServer();
			TileEntity tileEntity = worldServer.getTileEntity(position);
			if (tileEntity != null) {
				sendPacket(player, tileEntity.getUpdatePacket());
			}
		}
	}
}
