package net.imprex.orebfuscator.nms.v1_17_R1;

import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_17_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_17_R1.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_17_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractBlockState;
import net.imprex.orebfuscator.nms.AbstractNmsManager;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.resources.ResourceLocation;
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
		return level.getChunkProvider().isChunkLoaded(chunkX, chunkZ);
	}

	private static BlockState getBlockData(World world, int x, int y, int z, boolean loadChunk) {
		ServerLevel level = level(world);
		ServerChunkCache serverChunkCache = level.getChunkProvider();

		if (isChunkLoaded(level, x >> 4, z >> 4) || loadChunk) {
			// will load chunk if not loaded already
			LevelChunk chunk = serverChunkCache.getChunk(x >> 4, z >> 4, true);
			return chunk != null ? chunk.getBlockState(new BlockPos(x, y, z)) : null;
		}
		return null;
	}

	static int getBlockId(BlockState blockData) {
		if (blockData == null) {
			return 0;
		} else {
			int id = Block.BLOCK_STATE_REGISTRY.getId(blockData);
			return id == -1 ? 0 : id;
		}
	}

	public NmsManager(Config config) {
		super(config);

		for (BlockState blockData : Block.BLOCK_STATE_REGISTRY) {
			Material material = CraftBlockData.fromData(blockData).getMaterial();
			int blockId = getBlockId(blockData);
			this.registerMaterialId(material, blockId);
			this.setBlockFlags(blockId, blockData.isAir(), material.isOccluding(), blockData.hasBlockEntity());
		}
	}

	@Override
	protected AbstractRegionFileCache<?> createRegionFileCache(CacheConfig cacheConfig) {
		return new RegionFileCache(cacheConfig);
	}

	@Override
	public int getBitsPerBlock() {
		return Mth.ceillog2(Block.BLOCK_STATE_REGISTRY.size());
	}

	@Override
	public int getTotalBlockCount() {
		return Block.BLOCK_STATE_REGISTRY.size();
	}

	@Override
	public Optional<Material> getMaterialByName(String name) {
		Optional<Block> block = Registry.BLOCK.getOptional(new ResourceLocation(name));
		if (block.isPresent()) {
			return Optional.ofNullable(CraftMagicNumbers.getMaterial(block.get()));
		}
		return Optional.empty();
	}

	@Override
	public Optional<String> getNameByMaterial(Material material) {
		ResourceLocation resourceLocation = Registry.BLOCK.getKey(CraftMagicNumbers.getBlock(material));
		if (resourceLocation != null) {
			return Optional.of(resourceLocation.toString());
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
		ServerChunkCache serverChunkCache = level(world).getChunkProvider();
		LevelChunk chunk = serverChunkCache.getChunk(chunkX, chunkZ, true);
		return new ReadOnlyChunkWrapper(chunk);
	}

	@Override
	public AbstractBlockState<?> getBlockState(World world, int x, int y, int z) {
		BlockState blockData = getBlockData(world, x, y, z, false);	
		return blockData != null ? new BlockStateWrapper(x, y, z, world, blockData) : null;
	}

	@Override
	public int loadChunkAndGetBlockId(World world, int x, int y, int z) {
		BlockState blockData = getBlockData(world, x, y, z, true);
		return blockData != null ? getBlockId(blockData) : -1;
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
			ServerLevel ServerLevel = player.getLevel();
			BlockEntity BlockEntity = ServerLevel.getBlockEntity(position);
			if (BlockEntity != null) {
				player.connection.send(BlockEntity.getUpdatePacket());
			}
		}
	}
}
