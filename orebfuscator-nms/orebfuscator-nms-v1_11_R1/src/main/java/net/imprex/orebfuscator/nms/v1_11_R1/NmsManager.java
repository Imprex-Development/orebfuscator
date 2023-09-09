package net.imprex.orebfuscator.nms.v1_11_R1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_11_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_11_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_11_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.google.common.collect.ImmutableList;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractBlockState;
import net.imprex.orebfuscator.nms.AbstractNmsManager;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.BlockProperties;
import net.imprex.orebfuscator.util.BlockStateProperties;
import net.imprex.orebfuscator.util.NamespacedKey;
import net.minecraft.server.v1_11_R1.Block;
import net.minecraft.server.v1_11_R1.BlockAir;
import net.minecraft.server.v1_11_R1.BlockPosition;
import net.minecraft.server.v1_11_R1.Chunk;
import net.minecraft.server.v1_11_R1.ChunkProviderServer;
import net.minecraft.server.v1_11_R1.EntityPlayer;
import net.minecraft.server.v1_11_R1.IBlockData;
import net.minecraft.server.v1_11_R1.MathHelper;
import net.minecraft.server.v1_11_R1.MinecraftKey;
import net.minecraft.server.v1_11_R1.Packet;
import net.minecraft.server.v1_11_R1.PacketListenerPlayOut;
import net.minecraft.server.v1_11_R1.TileEntity;
import net.minecraft.server.v1_11_R1.WorldServer;

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
			Chunk chunk = chunkProviderServer.getChunkAt(x >> 4, z >> 4);
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

		for (MinecraftKey key : Block.REGISTRY.keySet()) {
			NamespacedKey namespacedKey = NamespacedKey.fromString(key.toString());

			Block block = Block.REGISTRY.get(key);
			Material material = CraftMagicNumbers.getMaterial(block);

			ImmutableList<IBlockData> possibleBlockStates = block.s().a();
			List<BlockStateProperties> possibleBlockStateProperties = new ArrayList<>();

			for (IBlockData blockState : possibleBlockStates) {
	
				BlockStateProperties properties = BlockStateProperties.builder(getBlockId(blockState))
						.withIsAir(block instanceof BlockAir)
						.withIsOccluding(material.isOccluding())
						.withIsBlockEntity(block.isTileEntity())
						.build();

				possibleBlockStateProperties.add(properties);
				this.registerBlockStateProperties(properties);
			}

			int defaultBlockStateId = getBlockId(block.getBlockData());
			BlockStateProperties defaultBlockState = getBlockStateProperties(defaultBlockStateId);

			BlockProperties blockProperties = BlockProperties.builder(namespacedKey)
				.withDefaultBlockState(defaultBlockState)
				.withPossibleBlockStates(ImmutableList.copyOf(possibleBlockStateProperties))
				.build();
			
			this.registerBlockProperties(blockProperties);
		}
	}

	@Override
	protected AbstractRegionFileCache<?> createRegionFileCache(CacheConfig cacheConfig) {
		return new RegionFileCache(cacheConfig);
	}

	@Override
	public int getMaxBitsPerBlock() {
		return MathHelper.d(Block.REGISTRY_ID.a());
	}

	@Override
	public int getTotalBlockCount() {
		return Block.REGISTRY_ID.a();
	}

	@Override
	public ReadOnlyChunk getReadOnlyChunk(World world, int chunkX, int chunkZ) {
		ChunkProviderServer chunkProviderServer = world(world).getChunkProviderServer();
		Chunk chunk = chunkProviderServer.getChunkAt(chunkX, chunkZ);
		return new ReadOnlyChunkWrapper(chunk);
	}

	@Override
	public AbstractBlockState<?> getBlockState(World world, int x, int y, int z) {
		IBlockData blockData = getBlockData(world, x, y, z, false);
		return blockData != null ? new BlockState(x, y, z, world, blockData) : null;
	}

	@Override
	public void sendBlockUpdates(Player player, Iterable<BlockPos> iterable) {
		EntityPlayer serverPlayer = player(player);
		WorldServer level = serverPlayer.x();

		BlockPosition.MutableBlockPosition position = new BlockPosition.MutableBlockPosition();
		Map<ChunkCoordIntPair, List<MultiBlockChangeInfo>> sectionPackets = new HashMap<>();
		List<Packet<PacketListenerPlayOut>> blockEntityPackets = new ArrayList<>();

		for (net.imprex.orebfuscator.util.BlockPos pos : iterable) {
			if (!isChunkLoaded(level, pos.x >> 4, pos.z >> 4)) {
				continue;
			}

			position.c(pos.x, pos.y, pos.z);
			IBlockData blockState = level.getType(position);

			ChunkCoordIntPair chunkCoord = new ChunkCoordIntPair(pos.x >> 4, pos.z >> 4);
			short location = (short) ((pos.x & 0xF) << 12 | (pos.z & 0xF) << 8 | pos.y);

			sectionPackets.computeIfAbsent(chunkCoord, key -> new ArrayList<>())
				.add(new MultiBlockChangeInfo(location, WrappedBlockData.fromHandle(blockState), chunkCoord));

			if (blockState.getBlock().isTileEntity()) {
				TileEntity blockEntity = level.getTileEntity(position);
				if (blockEntity != null) {
					blockEntityPackets.add(blockEntity.getUpdatePacket());
				}
			}
		}

		for (Map.Entry<ChunkCoordIntPair, List<MultiBlockChangeInfo>> entry : sectionPackets.entrySet()) {
			List<MultiBlockChangeInfo> blockStates = entry.getValue();
			if (blockStates.size() == 1) {
				MultiBlockChangeInfo blockEntry = blockStates.get(0);
				var blockPosition = new com.comphenix.protocol.wrappers.BlockPosition(
						blockEntry.getAbsoluteX(), blockEntry.getY(), blockEntry.getAbsoluteZ());

				PacketContainer packet = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);
				packet.getBlockPositionModifier().write(0, blockPosition);
				packet.getBlockData().write(0, blockEntry.getData());
				serverPlayer.playerConnection.sendPacket((Packet<?>) packet.getHandle());
			} else {
				PacketContainer packet = new PacketContainer(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
				packet.getChunkCoordIntPairs().write(0, entry.getKey());
				packet.getMultiBlockChangeInfoArrays().write(0, blockStates.toArray(MultiBlockChangeInfo[]::new));
				serverPlayer.playerConnection.sendPacket((Packet<?>) packet.getHandle());
			}
		}

		for (Packet<PacketListenerPlayOut> packet : blockEntityPackets) {
			serverPlayer.playerConnection.sendPacket(packet);
		}
	}
}
