package dev.imprex.orebfuscator.cache;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import dev.imprex.orebfuscator.statistics.CacheStatistics;
import dev.imprex.orebfuscator.util.ChunkCacheKey;

@NullMarked
public class ChunkSerializer {

  private static final int CACHE_VERSION = 3;

  private final AbstractRegionFileCache<?> regionFileCache;
  private final CacheStatistics statistics;

  public ChunkSerializer(AbstractRegionFileCache<?> regionFileCache, CacheStatistics statistics) {
    this.regionFileCache = regionFileCache;
    this.statistics = statistics;
  }

  @Nullable
  public ChunkCacheEntry read(ChunkCacheKey key) throws IOException {
    try (DataInputStream dataInputStream = this.regionFileCache.createInputStream(key)) {
      // check if cache entry has right version and if chunk is present
      if (dataInputStream == null || dataInputStream.readInt() != CACHE_VERSION || !dataInputStream.readBoolean()) {
        statistics.onDiskCacheRead(5);
        return null;
      }

      byte[] compressedData = new byte[dataInputStream.readInt()];
      dataInputStream.readFully(compressedData);

      statistics.onDiskCacheRead(9 + compressedData.length);

      return new ChunkCacheEntry(key, compressedData);
    } catch (IOException e) {
      throw new IOException("Unable to read chunk: " + key, e);
    }
  }

  public void write(ChunkCacheKey key, @Nullable ChunkCacheEntry value) throws IOException {
    try (DataOutputStream dataOutputStream = this.regionFileCache.createOutputStream(key)) {
      dataOutputStream.writeInt(CACHE_VERSION);
      // TODO: merge present boolean (and future flags) into the int32 version field wher int16 for version and int16 for flags

      if (value != null) {
        dataOutputStream.writeBoolean(true);

        byte[] compressedData = value.compressedData();
        dataOutputStream.writeInt(compressedData.length);
        dataOutputStream.write(compressedData);
        statistics.onDiskCacheWrite(9 + compressedData.length);
      } else {
        dataOutputStream.writeBoolean(false);
        statistics.onDiskCacheWrite(5);
      }
    } catch (IOException e) {
      throw new IOException("Unable to write chunk: " + key, e);
    }
  }
}
