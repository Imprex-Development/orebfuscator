package net.imprex.orebfuscator.nms;

import org.bukkit.World;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.util.BlockProperties;
import net.imprex.orebfuscator.util.NamespacedKey;

public interface NmsManager {

	AbstractRegionFileCache<?> getRegionFileCache();

	int getMaxBitsPerBlock();

	int getTotalBlockCount();

	BlockProperties getBlockByName(NamespacedKey key);

	boolean isAir(int blockId);

	boolean isOccluding(int blockId);

	boolean isBlockEntity(int blockId);

	ReadOnlyChunk getReadOnlyChunk(World world, int chunkX, int chunkZ);

	BlockStateHolder getBlockState(World world, int x, int y, int z);

	boolean sendBlockChange(Player player, int x, int y, int z);

	void close();
}