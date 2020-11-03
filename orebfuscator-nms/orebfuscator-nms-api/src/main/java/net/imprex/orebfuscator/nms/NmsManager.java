package net.imprex.orebfuscator.nms;

import java.util.Optional;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.util.BlockPos;

public interface NmsManager {

	AbstractRegionFileCache<?> getRegionFileCache();

	int getBitsPerBlock();

	int getTotalBlockCount();

	Optional<Material> getMaterialByName(String name);

	Optional<String> getNameByMaterial(Material material);

	Set<Integer> getMaterialIds(Material material);

	boolean isHoe(Material material);

	boolean isAir(int blockId);

	boolean isTileEntity(int blockId);

	// TODO use this in later versions
	boolean canApplyPhysics(Material material);

	BlockStateHolder getBlockState(World world, int x, int y, int z);

	int loadChunkAndGetBlockId(World world, int x, int y, int z);

	boolean sendBlockChange(Player player, BlockPos blockCoords);

	void close();
}