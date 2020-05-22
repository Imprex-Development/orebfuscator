package net.imprex.orebfuscator.nms;

import java.util.Optional;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.util.BlockCoords;

public interface NmsManager {

	AbstractRegionFileCache<?> getRegionFileCache();

	int getBitsPerBlock();

	int getMaterialSize();

	Optional<Material> getMaterialByName(String name);

	Set<Integer> getMaterialIds(Material material);

	int getCaveAirBlockId();

	boolean isHoe(Material material);

	boolean isAir(int blockId);

	boolean isTileEntity(int blockId);

	boolean canApplyPhysics(Material material);

	void updateBlockTileEntity(Player player, BlockCoords blockCoord);

	int getBlockLightLevel(World world, int x, int y, int z);

	BlockStateHolder getBlockState(World world, int x, int y, int z);

	int loadChunkAndGetBlockId(World world, int x, int y, int z);

	boolean sendBlockChange(Player player, BlockCoords blockCoords);

	void close();
}