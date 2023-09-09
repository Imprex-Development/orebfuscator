package net.imprex.orebfuscator.nms.v1_16_R3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.comphenix.protocol.events.PacketContainer;
import com.google.common.collect.ImmutableList;

import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractNmsManager;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.BlockProperties;
import net.imprex.orebfuscator.util.BlockStateProperties;
import net.imprex.orebfuscator.util.NamespacedKey;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Blocks;
import net.minecraft.server.v1_16_R3.Chunk;
import net.minecraft.server.v1_16_R3.ChunkProviderServer;
import net.minecraft.server.v1_16_R3.ChunkSection;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.Packet;
import net.minecraft.server.v1_16_R3.PacketListenerPlayOut;
import net.minecraft.server.v1_16_R3.PacketPlayOutBlockChange;
import net.minecraft.server.v1_16_R3.PacketPlayOutMultiBlockChange;
import net.minecraft.server.v1_16_R3.ResourceKey;
import net.minecraft.server.v1_16_R3.SectionPosition;
import net.minecraft.server.v1_16_R3.TileEntity;
import net.minecraft.server.v1_16_R3.WorldServer;

public class NmsManager extends AbstractNmsManager {

	private static final int BLOCK_ID_AIR = Block.getCombinedId(Blocks.AIR.getBlockData());

	static int getBlockState(Chunk chunk, int x, int y, int z) {
		ChunkSection[] sections = chunk.getSections();

		int sectionIndex = y >> 4;
		if (sectionIndex >= 0 && sectionIndex < sections.length) {
			ChunkSection section = sections[sectionIndex];
			if (section != null && !ChunkSection.a(section)) {
				return Block.getCombinedId(section.getType(x & 0xF, y & 0xF, z & 0xF));
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

	public NmsManager(Config config) {
		super(Block.REGISTRY_ID.a(), new RegionFileCache(config.cache()));

		for (Map.Entry<ResourceKey<Block>, Block> entry : IRegistry.BLOCK.d()) {
			NamespacedKey namespacedKey = NamespacedKey.fromString(entry.getKey().a().toString());
			Block block = entry.getValue();

			ImmutableList<IBlockData> possibleBlockStates = block.getStates().a();
			List<BlockStateProperties> possibleBlockStateProperties = new ArrayList<>();

			for (IBlockData blockState : possibleBlockStates) {
				Material material = CraftBlockData.fromData(blockState).getMaterial();

				BlockStateProperties properties = BlockStateProperties.builder(Block.getCombinedId(blockState))
						.withIsAir(blockState.isAir())
						/**
						* l -> for barrier/slime_block/spawner/leaves
						* isOccluding -> for every other block
						*/
						.withIsOccluding(material.isOccluding() && blockState.l()/*canOcclude*/)
						.withIsBlockEntity(block.isTileEntity())
						.build();

				possibleBlockStateProperties.add(properties);
				this.registerBlockStateProperties(properties);
			}

			int defaultBlockStateId = Block.getCombinedId(block.getBlockData());
			BlockStateProperties defaultBlockState = getBlockStateProperties(defaultBlockStateId);

			BlockProperties blockProperties = BlockProperties.builder(namespacedKey)
				.withDefaultBlockState(defaultBlockState)
				.withPossibleBlockStates(ImmutableList.copyOf(possibleBlockStateProperties))
				.build();
			
			this.registerBlockProperties(blockProperties);
		}
	}

	@Override
	public ReadOnlyChunk getReadOnlyChunk(World world, int chunkX, int chunkZ) {
		ChunkProviderServer chunkProviderServer = level(world).getChunkProvider();
		Chunk chunk = chunkProviderServer.getChunkAt(chunkX, chunkZ, true);
		return new ReadOnlyChunkWrapper(chunk);
	}

	@Override
	public int getBlockState(World world, int x, int y, int z) {
		ChunkProviderServer serverChunkCache = level(world).getChunkProvider();
		if (!serverChunkCache.isChunkLoaded(x >> 4, z >> 4)) {
			return BLOCK_ID_AIR;
		}

		Chunk chunk = serverChunkCache.getChunkAt(x >> 4, z >> 4, true);
		if (chunk == null) {
			return BLOCK_ID_AIR;
		}

		return getBlockState(chunk, x, y, z);
	}

	@Override
	public void sendBlockUpdates(World world, Iterable<net.imprex.orebfuscator.util.BlockPos> iterable) {
		ChunkProviderServer serverChunkCache = level(world).getChunkProvider();
		BlockPosition.MutableBlockPosition position = new BlockPosition.MutableBlockPosition();

		for (net.imprex.orebfuscator.util.BlockPos pos : iterable) {
			position.c(pos.x, pos.y, pos.z);
			serverChunkCache.flagDirty(position);
		}
	}

	@Override
	public void sendBlockUpdates(Player player, Iterable<BlockPos> iterable) {
		EntityPlayer serverPlayer = player(player);
		WorldServer level = serverPlayer.getWorldServer();
		ChunkProviderServer serverChunkCache = level.getChunkProvider();

		BlockPosition.MutableBlockPosition position = new BlockPosition.MutableBlockPosition();
		Map<SectionPosition, Short2ObjectMap<IBlockData>> sectionPackets = new HashMap<>();
		List<Packet<PacketListenerPlayOut>> blockEntityPackets = new ArrayList<>();

		for (net.imprex.orebfuscator.util.BlockPos pos : iterable) {
			if (!serverChunkCache.isChunkLoaded(pos.x >> 4, pos.z >> 4)) {
				continue;
			}

			position.c(pos.x, pos.y, pos.z);
			IBlockData blockState = level.getType(position);

			sectionPackets.computeIfAbsent(SectionPosition.a(position), key -> new Short2ObjectOpenHashMap<>())
				.put(SectionPosition.b(position), blockState);

			if (blockState.getBlock().isTileEntity()) {
				TileEntity blockEntity = level.getTileEntity(position);
				if (blockEntity != null) {
					blockEntityPackets.add(blockEntity.getUpdatePacket());
				}
			}
		}

		for (Map.Entry<SectionPosition, Short2ObjectMap<IBlockData>> entry : sectionPackets.entrySet()) {
			Short2ObjectMap<IBlockData> blockStates = entry.getValue();
			if (blockStates.size() == 1) {
				Short2ObjectMap.Entry<IBlockData> blockEntry = blockStates.short2ObjectEntrySet().iterator().next();
				BlockPosition blockPosition = entry.getKey().g(blockEntry.getShortKey());
				serverPlayer.playerConnection.sendPacket(new PacketPlayOutBlockChange(blockPosition, blockEntry.getValue()));
			} else {
				PacketContainer packet = PacketContainer.fromPacket(
						new PacketPlayOutMultiBlockChange(entry.getKey(), blockStates.keySet(), null, false));
				packet.getSpecificModifier(IBlockData[].class).write(0, blockStates.values().toArray(IBlockData[]::new));
				serverPlayer.playerConnection.sendPacket((Packet<?>) packet.getHandle());
			}
		}

		for (Packet<PacketListenerPlayOut> packet : blockEntityPackets) {
			serverPlayer.playerConnection.sendPacket(packet);
		}
	}
}
