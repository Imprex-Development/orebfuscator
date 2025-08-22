package net.imprex.orebfuscator.nms;

import org.bukkit.World;
import org.bukkit.entity.Player;

import dev.imprex.orebfuscator.cache.AbstractRegionFileCache;
import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.util.BlockPos;

public interface NmsManager extends RegistryAccessor {

	AbstractRegionFileCache<?> getRegionFileCache();

	ReadOnlyChunk getReadOnlyChunk(World world, int chunkX, int chunkZ);

	int getBlockState(World world, int x, int y, int z);

	void sendBlockUpdates(World world, Iterable<BlockPos> iterable);

	void sendBlockUpdates(Player player, Iterable<BlockPos> iterable);

	void close();
}