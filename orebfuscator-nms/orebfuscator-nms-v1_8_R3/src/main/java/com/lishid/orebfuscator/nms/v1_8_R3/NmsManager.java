/**
 * @author lishid
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.nms.v1_8_R3;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.util.CraftChatMessage;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.lishid.orebfuscator.nms.IBlockInfo;
import com.lishid.orebfuscator.nms.IChunkCache;
import com.lishid.orebfuscator.nms.INBT;
import com.lishid.orebfuscator.nms.INmsManager;
import com.lishid.orebfuscator.types.ConfigDefaults;

import net.imprex.orebfuscator.util.BlockCoords;
import net.minecraft.server.v1_8_R3.Block;
import net.minecraft.server.v1_8_R3.BlockPosition;
import net.minecraft.server.v1_8_R3.Chunk;
import net.minecraft.server.v1_8_R3.ChunkProviderServer;
import net.minecraft.server.v1_8_R3.IBlockData;
import net.minecraft.server.v1_8_R3.IChatBaseComponent;
import net.minecraft.server.v1_8_R3.Packet;
import net.minecraft.server.v1_8_R3.PacketPlayOutBlockChange;
import net.minecraft.server.v1_8_R3.TileEntity;
import net.minecraft.server.v1_8_R3.WorldServer;

public class NmsManager implements INmsManager {

	private final ConfigDefault configDefaults;
	private final ProtocolManager protocolManager;

	private int maxLoadedCacheFiles;

	public NmsManager() {
		this.protocolManager = ProtocolLibrary.getProtocolManager();
		this.configDefaults = new ConfigDefault();
	}

	@Override
	public ConfigDefaults getConfigDefaults() {
		return this.configDefaults;
	}

	@Override
	public Material[] getExtraTransparentBlocks() {
		return this.configDefaults.extraTransparentBlocks;
	}

	@Override
	public void setMaxLoadedCacheFiles(int value) {
		this.maxLoadedCacheFiles = value;
	}

	@Override
	public INBT createNBT() {
		return new NBT();
	}

	@Override
	public IChunkCache createChunkCache() {
		return new ChunkCache(this.maxLoadedCacheFiles);
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

		((CraftWorld) world).getHandle().notify(blockPosition);
	}

	@Override
	public int getBlockLightLevel(World world, int x, int y, int z) {
		return ((CraftWorld) world).getHandle().getLightLevel(new BlockPosition(x, y, z));
	}

	@Override
	public IBlockInfo getBlockInfo(World world, int x, int y, int z) {
		IBlockData blockData = getBlockData(world, x, y, z, false);

		return blockData != null ? new BlockInfo(x, y, z, blockData) : null;
	}

	@Override
	public int loadChunkAndGetBlockId(World world, int x, int y, int z) {
		IBlockData blockData = getBlockData(world, x, y, z, true);
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

	@Override
	public Set<Integer> getMaterialIds(Material material) {
		return this.configDefaults.getMaterialIds(material);
	}

	@Override
	public boolean sendBlockChange(Player player, Location location) {
		IBlockData blockData = this.getBlockData(location.getWorld(), location.getBlockX(), location.getBlockY(),
				location.getBlockZ(), false);

		if (blockData == null)
			return false;

		PacketPlayOutBlockChange packet = new PacketPlayOutBlockChange(((CraftWorld) location.getWorld()).getHandle(),
				new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
		packet.block = blockData;

		((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
		return true;
	}

	// PacketPlayOutMultiBlockChange
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

		protocolManager.sendServerPacket(player, packet);
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
		ChunkProviderServer chunkProviderServer = worldServer.chunkProviderServer;

		if (!loadChunk && !chunkProviderServer.isChunkLoaded(chunkX, chunkZ))
			return null;

		return chunkProviderServer.getOrCreateChunk(chunkX, chunkZ);
	}
}