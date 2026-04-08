package net.imprex.orebfuscator.nms.v26_1;

import dev.imprex.orebfuscator.cache.AbstractRegionFileCache;
import dev.imprex.orebfuscator.config.api.CacheConfig;
import dev.imprex.orebfuscator.util.ChunkCacheKey;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.levelgen.WorldDimensions;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class RegionFileCache extends AbstractRegionFileCache<RegionFile> {

  RegionFileCache(CacheConfig cacheConfig) {
    super(cacheConfig);
  }

  @Override
  protected RegionFile createRegionFile(Path path) throws IOException {
    boolean isSyncChunkWrites = serverHandle(Bukkit.getServer(), DedicatedServer.class).forceSynchronousWrites();
    RegionStorageInfo info = new RegionStorageInfo("orebfuscator", Level.OVERWORLD, "orebfuscator_cache");
    return new RegionFile(info, path, path.getParent(), RegionFileVersion.VERSION_NONE, isSyncChunkWrites);
  }

  @Override
  protected void closeRegionFile(RegionFile t) throws IOException {
    t.close();
  }

  @Override
  protected @Nullable DataInputStream createInputStream(RegionFile t, ChunkCacheKey key) throws IOException {
    return t.getChunkDataInputStream(new ChunkPos(key.x(), key.z()));
  }

  @Override
  protected DataOutputStream createOutputStream(RegionFile t, ChunkCacheKey key) throws IOException {
    return t.getChunkDataOutputStream(new ChunkPos(key.x(), key.z()));
  }
}