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
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Chunk;
import net.minecraft.server.v1_16_R3.ChunkProviderServer;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.Packet;
import net.minecraft.server.v1_16_R3.PacketListenerPlayOut;
import net.minecraft.server.v1_16_R3.PacketPlayOutBlockChange;
import net.minecraft.server.v1_16_R3.PacketPlayOutMultiBlockChange;
import net.minecraft.server.v1_16_R3.ResourceKey;
import net.minecraft.server.v1_16_R3.SectionPosition;
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

	public NmsManager(Config config) {
		super(config);

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
	protected AbstractRegionFileCache<?> createRegionFileCache(CacheConfig cacheConfig) {
		return new RegionFileCache(cacheConfig);
	}

	@Override
	public int getMaxBitsPerBlock() {
		return MathHelper.e(Block.REGISTRY_ID.a());
	}

	@Override
	public int getTotalBlockCount() {
		return Block.REGISTRY_ID.a();
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
	public void sendBlockUpdates(Player player, Iterable<BlockPos> iterable) {
		EntityPlayer serverPlayer = player(player);
		WorldServer level = serverPlayer.getWorldServer();

		BlockPosition.MutableBlockPosition position = new BlockPosition.MutableBlockPosition();
		Map<SectionPosition, Short2ObjectMap<IBlockData>> sectionPackets = new HashMap<>();
		List<Packet<PacketListenerPlayOut>> blockEntityPackets = new ArrayList<>();

		for (net.imprex.orebfuscator.util.BlockPos pos : iterable) {
			if (!isChunkLoaded(level, pos.x >> 4, pos.z >> 4)) {
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
