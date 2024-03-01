package net.imprex.orebfuscator.nms.v1_20_R2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_20_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_20_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.google.common.collect.ImmutableList;

import it.unimi.dsi.fastutil.shorts.Short2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractNmsManager;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.util.BlockProperties;
import net.imprex.orebfuscator.util.BlockStateProperties;
import net.imprex.orebfuscator.util.NamespacedKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class NmsManager extends AbstractNmsManager {

	private static final int BLOCK_ID_AIR = Block.getId(Blocks.AIR.defaultBlockState());

	static int getBlockState(LevelChunk chunk, int x, int y, int z) {
		LevelChunkSection[] sections = chunk.getSections();

		int sectionIndex = chunk.getSectionIndex(y);
		if (sectionIndex >= 0 && sectionIndex < sections.length) {
			LevelChunkSection section = sections[sectionIndex];
			if (section != null && !section.hasOnlyAir()) {
				return Block.getId(section.getBlockState(x & 0xF, y & 0xF, z & 0xF));
			}
		}

		return BLOCK_ID_AIR;
	}

	private static ServerLevel level(World world) {
		return ((CraftWorld) world).getHandle();
	}

	private static ServerPlayer player(Player player) {
		return ((CraftPlayer) player).getHandle();
	}

	public NmsManager(Config config) {
		super(Block.BLOCK_STATE_REGISTRY.size(), new RegionFileCache(config.cache()));

		for (Map.Entry<ResourceKey<Block>, Block> entry : BuiltInRegistries.BLOCK.entrySet()) {
			NamespacedKey namespacedKey = NamespacedKey.fromString(entry.getKey().location().toString());
			Block block = entry.getValue();

			ImmutableList<BlockState> possibleBlockStates = block.getStateDefinition().getPossibleStates();
			BlockProperties.Builder builder = BlockProperties.builder(namespacedKey);

			for (BlockState blockState : possibleBlockStates) {
				Material material = CraftBlockData.fromData(blockState).getMaterial();

				BlockStateProperties properties = BlockStateProperties.builder(Block.getId(blockState))
						.withIsAir(blockState.isAir())
						// check if material is occluding and use blockData check for rare edge cases like barrier, spawner, slime_block, ...
						.withIsOccluding(material.isOccluding() && blockState.canOcclude())
						.withIsBlockEntity(blockState.hasBlockEntity())
						.withIsDefaultState(Objects.equals(block.defaultBlockState(), blockState))
						.build();

				builder.withBlockState(properties);
			}

			this.registerBlockProperties(builder.build());
		}
	}

	@Override
	public ReadOnlyChunk getReadOnlyChunk(World world, int chunkX, int chunkZ) {
		ServerChunkCache serverChunkCache = level(world).getChunkSource();
		LevelChunk chunk = serverChunkCache.getChunk(chunkX, chunkZ, true);
		return new ReadOnlyChunkWrapper(chunk);
	}

	@Override
	public int getBlockState(World world, int x, int y, int z) {
		ServerChunkCache serverChunkCache = level(world).getChunkSource();
		if (!serverChunkCache.isChunkLoaded(x >> 4, z >> 4)) {
			return BLOCK_ID_AIR;
		}

		LevelChunk chunk = serverChunkCache.getChunk(x >> 4, z >> 4, true);
		if (chunk == null) {
			return BLOCK_ID_AIR;
		}

		return getBlockState(chunk, x, y, z);
	}

	@Override
	public void sendBlockUpdates(World world, Iterable<net.imprex.orebfuscator.util.BlockPos> iterable) {
		ServerChunkCache serverChunkCache = level(world).getChunkSource();
		BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

		for (net.imprex.orebfuscator.util.BlockPos pos : iterable) {
			position.set(pos.x, pos.y, pos.z);
			serverChunkCache.blockChanged(position);
		}
	}

	@Override
	public void sendBlockUpdates(Player player, Iterable<net.imprex.orebfuscator.util.BlockPos> iterable) {
		ServerPlayer serverPlayer = player(player);
		ServerLevel level = serverPlayer.serverLevel();
		ServerChunkCache serverChunkCache = level.getChunkSource();

		BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
		Map<SectionPos, Short2ObjectMap<BlockState>> sectionPackets = new HashMap<>();
		List<Packet<ClientGamePacketListener>> blockEntityPackets = new ArrayList<>();

		for (net.imprex.orebfuscator.util.BlockPos pos : iterable) {
			if (!serverChunkCache.isChunkLoaded(pos.x >> 4, pos.z >> 4)) {
				continue;
			}

			position.set(pos.x, pos.y, pos.z);
			BlockState blockState = level.getBlockState(position);

			sectionPackets.computeIfAbsent(SectionPos.of(position), key -> new Short2ObjectLinkedOpenHashMap<>())
				.put(SectionPos.sectionRelativePos(position), blockState);

			if (blockState.hasBlockEntity()) {
				BlockEntity blockEntity = level.getBlockEntity(position);
				if (blockEntity != null) {
					blockEntityPackets.add(blockEntity.getUpdatePacket());
				}
			}
		}

		for (Map.Entry<SectionPos, Short2ObjectMap<BlockState>> entry : sectionPackets.entrySet()) {
			Short2ObjectMap<BlockState> blockStates = entry.getValue();
			if (blockStates.size() == 1) {
				Short2ObjectMap.Entry<BlockState> blockEntry = blockStates.short2ObjectEntrySet().iterator().next();
				BlockPos blockPosition = entry.getKey().relativeToBlockPos(blockEntry.getShortKey());
				serverPlayer.connection.send(new ClientboundBlockUpdatePacket(blockPosition, blockEntry.getValue()));
			} else {
				serverPlayer.connection.send(new ClientboundSectionBlocksUpdatePacket(entry.getKey(),
						blockStates.keySet(), blockStates.values().toArray(BlockState[]::new)));
			}
		}

		for (Packet<ClientGamePacketListener> packet : blockEntityPackets) {
			serverPlayer.connection.send(packet);
		}
	}
}
