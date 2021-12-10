package net.imprex.orebfuscator.nms;

import java.util.Optional;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;

public interface NmsManager {

	AbstractRegionFileCache<?> getRegionFileCache();

	int getBitsPerBlock();

	int getTotalBlockCount();

	Optional<Material> getMaterialByName(String name);

	Optional<String> getNameByMaterial(Material material);

	Set<Integer> getBlockIds(Material material);

	boolean isHoe(Material material);

	boolean isAir(int blockId);

	boolean isOccluding(int blockId);

	boolean isTileEntity(int blockId);

	ReadOnlyChunk getReadOnlyChunk(World world, int chunkX, int chunkZ);

	BlockStateHolder getBlockState(World world, int x, int y, int z);

	int loadChunkAndGetBlockId(World world, int x, int y, int z);

	boolean sendBlockChange(Player player, int x, int y, int z);

	void close();
}