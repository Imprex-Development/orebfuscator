/**
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.nms;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.util.BlockCoords;

public interface INmsManager {

	AbstractRegionFileCache<?> getRegionFileCache();

	void updateBlockTileEntity(BlockCoords blockCoord, Player player);

	void notifyBlockChange(World world, IBlockInfo blockInfo);

	int getBlockLightLevel(World world, int x, int y, int z);

	IBlockInfo getBlockInfo(World world, int x, int y, int z);

	int loadChunkAndGetBlockId(World world, int x, int y, int z);

	String getTextFromChatComponent(String json);

	boolean isHoe(Material item);

	boolean isSign(int combinedBlockId);

	boolean isAir(int combinedBlockId);

	boolean isTileEntity(int combinedBlockId);

	int getCaveAirBlockId();

	int getBitsPerBlock();

	boolean canApplyPhysics(Material blockMaterial);

	int getMaterialSize();

	Set<Integer> getMaterialIds(Material material);

	boolean sendBlockChange(Player player, Location blockLocation);

	void sendMultiBlockChange(Player player, int chunkX, int chunkZ, Location... locations) throws IllegalAccessException, InvocationTargetException;

	boolean hasLightArray();

	boolean hasBlockCount();

	void close();
}