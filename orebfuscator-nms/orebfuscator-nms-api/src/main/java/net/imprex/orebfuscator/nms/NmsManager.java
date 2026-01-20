package net.imprex.orebfuscator.nms;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.Nullable;
import dev.imprex.orebfuscator.cache.AbstractRegionFileCache;
import dev.imprex.orebfuscator.config.api.Config;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.util.BlockPos;

public interface NmsManager extends RegistryAccessor {

  AbstractRegionFileCache<?> createRegionFileCache(Config config);

  ChunkAccessor getChunkAccessor(World world, int chunkX, int chunkZ);

  // TODO: change to getChunkNow, implement in each version and add use in deobfuscation worker
  @Nullable ChunkAccessor tryGetChunkAccessor(World world, int chunkX, int chunkZ);

  int getBlockState(World world, int x, int y, int z);

  void sendBlockUpdates(World world, Iterable<BlockPos> iterable);

  void sendBlockUpdates(Player player, Iterable<BlockPos> iterable);
}