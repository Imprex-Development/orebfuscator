package net.imprex.orebfuscator.nms.v1_12_R1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.google.common.collect.ImmutableList;

import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractNmsManager;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.BlockProperties;
import net.imprex.orebfuscator.util.BlockStateProperties;
import net.imprex.orebfuscator.util.NamespacedKey;
import net.minecraft.server.v1_12_R1.Block;
import net.minecraft.server.v1_12_R1.BlockAir;
import net.minecraft.server.v1_12_R1.BlockPosition;
import net.minecraft.server.v1_12_R1.Blocks;
import net.minecraft.server.v1_12_R1.Chunk;
import net.minecraft.server.v1_12_R1.ChunkProviderServer;
import net.minecraft.server.v1_12_R1.ChunkSection;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.IBlockData;
import net.minecraft.server.v1_12_R1.MinecraftKey;
import net.minecraft.server.v1_12_R1.Packet;
import net.minecraft.server.v1_12_R1.PacketListenerPlayOut;
import net.minecraft.server.v1_12_R1.TileEntity;
import net.minecraft.server.v1_12_R1.WorldServer;

public class NmsManager extends AbstractNmsManager {

	private static final int BLOCK_ID_AIR = getBlockId(Blocks.AIR.getBlockData());

	static int getBlockState(Chunk chunk, int x, int y, int z) {
		ChunkSection[] sections = chunk.getSections();

		int sectionIndex = y >> 4;
		if (sectionIndex >= 0 && sectionIndex < sections.length) {
			ChunkSection section = sections[sectionIndex];
			if (section != null && section != Chunk.a) {
				return getBlockId(section.getType(x & 0xF, y & 0xF, z & 0xF));
			}
		}

		return BLOCK_ID_AIR;
	}

	private static WorldServer level(World world) {
		return ((CraftWorld) world).getHandle();
	}

	private static EntityPlayer player(Player player) {
		return ((CraftPlayer) player).getHandle();
	}

	private static int getBlockId(IBlockData blockData) {
		if (blockData == null) {
			return 0;
		} else {
			int id = Block.REGISTRY_ID.getId(blockData);
			return id == -1 ? 0 : id;
		}
	}

	public NmsManager(Config config) {
		super(Block.REGISTRY_ID.a(), new RegionFileCache(config.cache()));

		for (MinecraftKey key : Block.REGISTRY.keySet()) {
			NamespacedKey namespacedKey = NamespacedKey.fromString(key.toString());
			Block block = Block.REGISTRY.get(key);

			ImmutableList<IBlockData> possibleBlockStates = block.s().a();
			BlockProperties.Builder builder = BlockProperties.builder(namespacedKey);

			for (IBlockData blockState : possibleBlockStates) {

				BlockStateProperties properties = BlockStateProperties.builder(getBlockId(blockState))
						.withIsAir(block instanceof BlockAir)
						/**
						 * p -> for barrier/slime_block/spawner
						 * r -> for every other block
						 */
						.withIsOccluding(blockState.p() && blockState.r()/*canOcclude*/)
						.withIsBlockEntity(block.isTileEntity())
						.withIsDefaultState(Objects.equals(block.getBlockData(), blockState))
						.build();

				builder.withBlockState(properties);
			}

			this.registerBlockProperties(builder.build());
		}
	}

	@Override
	public ReadOnlyChunk getReadOnlyChunk(World world, int chunkX, int chunkZ) {
		ChunkProviderServer chunkProviderServer = level(world).getChunkProviderServer();
		Chunk chunk = chunkProviderServer.getChunkAt(chunkX, chunkZ);
		return new ReadOnlyChunkWrapper(chunk);
	}

	@Override
	public int getBlockState(World world, int x, int y, int z) {
		ChunkProviderServer serverChunkCache = level(world).getChunkProviderServer();
		if (!serverChunkCache.isLoaded(x >> 4, z >> 4)) {
			return BLOCK_ID_AIR;
		}

		Chunk chunk = serverChunkCache.getChunkAt(x >> 4, z >> 4);
		if (chunk == null) {
			return BLOCK_ID_AIR;
		}

		return getBlockState(chunk, x, y, z);
	}

	@Override
	public void sendBlockUpdates(World world, Iterable<net.imprex.orebfuscator.util.BlockPos> iterable) {
		WorldServer level = level(world);
		BlockPosition.MutableBlockPosition position = new BlockPosition.MutableBlockPosition();

		for (net.imprex.orebfuscator.util.BlockPos pos : iterable) {
			position.c(pos.x, pos.y, pos.z);

			IBlockData blockState = level.getType(position);
			level.notify(position, blockState, blockState, 0);
		}
	}

	@Override
	public void sendBlockUpdates(Player player, Iterable<BlockPos> iterable) {
		EntityPlayer serverPlayer = player(player);
		WorldServer level = serverPlayer.x();
		ChunkProviderServer serverChunkCache = level.getChunkProviderServer();

		BlockPosition.MutableBlockPosition position = new BlockPosition.MutableBlockPosition();
		Map<ChunkCoordIntPair, List<MultiBlockChangeInfo>> sectionPackets = new HashMap<>();
		List<Packet<PacketListenerPlayOut>> blockEntityPackets = new ArrayList<>();

		for (net.imprex.orebfuscator.util.BlockPos pos : iterable) {
			if (!serverChunkCache.isLoaded(pos.x >> 4, pos.z >> 4)) {
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
