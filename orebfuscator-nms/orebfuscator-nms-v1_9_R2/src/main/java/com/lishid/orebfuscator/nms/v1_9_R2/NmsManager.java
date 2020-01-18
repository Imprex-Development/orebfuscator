/**
 * @author lishid
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.nms.v1_9_R2;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_9_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_9_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_9_R2.util.CraftChatMessage;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.google.common.collect.ImmutableList;
import com.lishid.orebfuscator.nms.IBlockInfo;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractNmsManager;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.nms.v1_9_R2.RegionFileCache;
import net.imprex.orebfuscator.util.BlockCoords;
import net.minecraft.server.v1_9_R2.Block;
import net.minecraft.server.v1_9_R2.BlockPosition;
import net.minecraft.server.v1_9_R2.Chunk;
import net.minecraft.server.v1_9_R2.ChunkProviderServer;
import net.minecraft.server.v1_9_R2.IBlockData;
import net.minecraft.server.v1_9_R2.IChatBaseComponent;
import net.minecraft.server.v1_9_R2.Packet;
import net.minecraft.server.v1_9_R2.PacketPlayOutBlockChange;
import net.minecraft.server.v1_9_R2.TileEntity;
import net.minecraft.server.v1_9_R2.WorldServer;

public class NmsManager extends AbstractNmsManager {

	private final ProtocolManager protocolManager;

	public NmsManager(Config config) {
		super(config);

		this.protocolManager = ProtocolLibrary.getProtocolManager();
	}

	@Override
	protected AbstractRegionFileCache<?> createRegionFileCache(CacheConfig cacheConfig) {
		return new RegionFileCache(cacheConfig);
	}

	@Override
	public int getMaterialSize() {
		return Block.REGISTRY_ID.a();
	}

	@Override
	public void updateBlockTileEntity(BlockCoords blockCoord, Player player) {
		CraftWorld world = (CraftWorld) player.getWorld();
		TileEntity tileEntity = world.getTileEntityAt(blockCoord.x, blockCoord.y, blockCoord.z);

		if (tileEntity == null) {
			return;
		}

		Packet<?> packet = tileEntity.getUpdatePacket();

		if (packet != null) {
			((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
		}
	}

	@Override
	public void notifyBlockChange(World world, IBlockInfo blockInfo) {
		BlockPosition blockPosition = new BlockPosition(blockInfo.getX(), blockInfo.getY(), blockInfo.getZ());
		IBlockData blockData = ((BlockInfo) blockInfo).getBlockData();

		((CraftWorld) world).getHandle().notify(blockPosition, blockData, blockData, 0);
	}

	@Override
	public int getBlockLightLevel(World world, int x, int y, int z) {
		return ((CraftWorld) world).getHandle().getLightLevel(new BlockPosition(x, y, z));
	}

	@Override
	public IBlockInfo getBlockInfo(World world, int x, int y, int z) {
		IBlockData blockData = this.getBlockData(world, x, y, z, false);

		return blockData != null ? new BlockInfo(x, y, z, blockData) : null;
	}

	@Override
	public int loadChunkAndGetBlockId(World world, int x, int y, int z) {
		IBlockData blockData = this.getBlockData(world, x, y, z, true);
		return blockData != null ? Block.getCombinedId(blockData) : -1;
	}

	@Override
	public String getTextFromChatComponent(String json) {
		IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a(json);
		return CraftChatMessage.fromComponent(component);
	}

	@Override
	public boolean isHoe(Material item) {
		return item == Material.WOOD_HOE || item == Material.IRON_HOE || item == Material.GOLD_HOE
				|| item == Material.DIAMOND_HOE;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean isSign(int combinedBlockId) {
		int typeId = combinedBlockId >> 4;

		return typeId == Material.WALL_SIGN.getId() || typeId == Material.SIGN_POST.getId();
	}

	@Override
	public boolean isAir(int combinedBlockId) {
		return combinedBlockId == 0;
	}

	@Override
	public boolean isTileEntity(int combinedBlockId) {
		return Block.getByCombinedId(combinedBlockId).getBlock().isTileEntity();
	}

	@Override
	public int getCaveAirBlockId() {
		return 0;
	}

	@Override
	public int getBitsPerBlock() {
		return 13;
	}

	@Override
	public boolean canApplyPhysics(Material blockMaterial) {
		return blockMaterial == Material.AIR || blockMaterial == Material.FIRE || blockMaterial == Material.WATER
				|| blockMaterial == Material.STATIONARY_WATER || blockMaterial == Material.LAVA
				|| blockMaterial == Material.STATIONARY_LAVA;
	}

	@SuppressWarnings("deprecation")
	@Override
	public Set<Integer> getMaterialIds(Material material) {
		Set<Integer> ids = new HashSet<>();
		int blockId = material.getId() << 4;
		Block block = Block.getById(material.getId());
		ImmutableList<IBlockData> blockDataList = block.t().a();

		for (IBlockData blockData : blockDataList) {
			ids.add(blockId | block.toLegacyData(blockData));
		}

		return ids;
	}

	@Override
	public boolean sendBlockChange(Player player, Location location) {
		IBlockData blockData = this.getBlockData(location.getWorld(), location.getBlockX(), location.getBlockY(),
				location.getBlockZ(), false);

		if (blockData == null) {
			return false;
		}

		PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(((CraftWorld) location.getWorld()).getHandle(),
				new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
		packet.block = blockData;

		((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
		return true;
	}

	// PacketPlayOutMultiBlockChange
	@Override
	public void sendMultiBlockChange(Player player, int chunkX, int chunkZ, Location... locations)
			throws IllegalAccessException, InvocationTargetException {
		if (locations.length == 0) {
			return;
		}

		PacketContainer packet = this.protocolManager.createPacket(PacketType.Play.Server.MULTI_BLOCK_CHANGE);
		MultiBlockChangeInfo[] blockInfoArray = new MultiBlockChangeInfo[locations.length];

		int index = 0;
		for (Location location : locations) {
			org.bukkit.block.Block block = location.getBlock();

			if (block == null || location.getBlockX() >> 4 != chunkX || location.getBlockZ() >> 4 != chunkZ) {
				index++;
				continue;
			}

			blockInfoArray[index++] = new MultiBlockChangeInfo(location, WrappedBlockData.createData(block.getType()));
		}

		packet.getChunkCoordIntPairs().write(0, new ChunkCoordIntPair(chunkX, chunkZ));
		packet.getMultiBlockChangeInfoArrays().write(0, blockInfoArray);

		this.protocolManager.sendServerPacket(player, packet);
	}

	@Override
	public boolean hasBlockCount() {
		return false;
	}

	@Override
	public boolean hasLightArray() {
		return true;
	}

	private IBlockData getBlockData(World world, int x, int y, int z, boolean loadChunk) {
		Chunk chunk = this.getChunk(world, x >> 4, z >> 4, loadChunk);

		return chunk != null ? chunk.getBlockData(new BlockPosition(x, y, z)) : null;
	}

	private Chunk getChunk(World world, int chunkX, int chunkZ, boolean loadChunk) {
		WorldServer worldServer = ((CraftWorld) world).getHandle();
		ChunkProviderServer chunkProviderServer = worldServer.getChunkProviderServer();

		if (!loadChunk && !chunkProviderServer.isLoaded(chunkX, chunkZ)) {
			return null;
		}

		return chunkProviderServer.getOrLoadChunkAt(chunkX, chunkZ);
	}
}