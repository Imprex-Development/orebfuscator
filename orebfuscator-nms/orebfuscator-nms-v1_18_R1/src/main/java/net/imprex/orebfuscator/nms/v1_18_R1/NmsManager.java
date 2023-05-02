package net.imprex.orebfuscator.nms.v1_18_R1;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_18_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_18_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.google.common.collect.ImmutableList;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractBlockState;
import net.imprex.orebfuscator.nms.AbstractNmsManager;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.util.BlockProperties;
import net.imprex.orebfuscator.util.BlockStateProperties;
import net.imprex.orebfuscator.util.NamespacedKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

public class NmsManager extends AbstractNmsManager {

	private static ServerLevel level(World world) {
		return ((CraftWorld) world).getHandle();
	}

	private static ServerPlayer player(Player player) {
		return ((CraftPlayer) player).getHandle();
	}

	private static boolean isChunkLoaded(ServerLevel level, int chunkX, int chunkZ) {
		return level.getChunkSource().isChunkLoaded(chunkX, chunkZ);
	}

	private static BlockState getBlockData(World world, int x, int y, int z, boolean loadChunk) {
		ServerLevel level = level(world);
		ServerChunkCache serverChunkCache = level.getChunkSource();

		if (isChunkLoaded(level, x >> 4, z >> 4) || loadChunk) {
			// will load chunk if not loaded already
			LevelChunk chunk = serverChunkCache.getChunk(x >> 4, z >> 4, true);
			return chunk != null ? chunk.getBlockState(new BlockPos(x, y, z)) : null;
		}
		return null;
	}

	public NmsManager(Config config) {
		super(config);


		for (Map.Entry<ResourceKey<Block>, Block> entry : Registry.BLOCK.entrySet()) {
			NamespacedKey namespacedKey = NamespacedKey.fromString(entry.getKey().location().toString());
			Block block = entry.getValue();

			ImmutableList<BlockState> possibleBlockStates = block.getStateDefinition().getPossibleStates();
			List<BlockStateProperties> possibleBlockStateProperties = new ArrayList<>();

			for (BlockState blockState : possibleBlockStates) {
				Material material = CraftBlockData.fromData(blockState).getMaterial();

				BlockStateProperties properties = BlockStateProperties.builder(Block.getId(blockState))
						.withIsAir(blockState.isAir())
						// check if material is occluding and use blockData check for rare edge cases like barrier, spawner, slime_block, ...
						.withIsOccluding(material.isOccluding() && blockState.canOcclude())
						.withIsBlockEntity(blockState.hasBlockEntity())
						.build();

				possibleBlockStateProperties.add(properties);
				this.registerBlockStateProperties(properties);
			}

			int defaultBlockStateId = Block.getId(block.defaultBlockState());
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
		return Mth.ceillog2(Block.BLOCK_STATE_REGISTRY.size());
	}

	@Override
	public int getTotalBlockCount() {
		return Block.BLOCK_STATE_REGISTRY.size();
	}

	@Override
	public ReadOnlyChunk getReadOnlyChunk(World world, int chunkX, int chunkZ) {
		ServerChunkCache serverChunkCache = level(world).getChunkSource();
		LevelChunk chunk = serverChunkCache.getChunk(chunkX, chunkZ, true);
		return new ReadOnlyChunkWrapper(chunk);
	}

	@Override
	public AbstractBlockState<?> getBlockState(World world, int x, int y, int z) {
		BlockState blockData = getBlockData(world, x, y, z, false);
		return blockData != null ? new BlockStateWrapper(x, y, z, world, blockData) : null;
	}

	@Override
	public boolean sendBlockChange(Player player, int x, int y, int z) {
		ServerPlayer serverPlayer = player(player);
		ServerLevel level = serverPlayer.getLevel();
		if (!isChunkLoaded(level, x >> 4, z >> 4)) {
			return false;
		}

		BlockPos position = new BlockPos(x, y, z);
		ClientboundBlockUpdatePacket packet = new ClientboundBlockUpdatePacket(level, position);
		serverPlayer.connection.send(packet);
		updateBlockEntity(serverPlayer, position, packet.blockState);

		return true;
	}

	private void updateBlockEntity(ServerPlayer player, BlockPos position, BlockState blockData) {
		if (blockData.hasBlockEntity()) {
			ServerLevel serverLevel = player.getLevel();
			BlockEntity blockEntity = serverLevel.getBlockEntity(position);
			if (blockEntity != null) {
				player.connection.send(blockEntity.getUpdatePacket());
			}
		}
	}
}
