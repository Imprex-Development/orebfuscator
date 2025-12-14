package net.imprex.orebfuscator.cache;

import dev.imprex.orebfuscator.cache.AbstractRegionFileCache;
import dev.imprex.orebfuscator.util.ChunkCacheKey;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record ChunkSerializer(AbstractRegionFileCache<?> regionFileCache) {

  private static final int CACHE_VERSION = 2;

  public @Nullable CacheChunkEntry read(ChunkCacheKey key) throws IOException {
    try (DataInputStream dataInputStream = this.regionFileCache.createInputStream(key)) {
      if (dataInputStream != null) {
        // check if cache entry has right version and if chunk is present
        if (dataInputStream.readInt() != CACHE_VERSION || !dataInputStream.readBoolean()) {
          return null;
        }

        byte[] compressedData = new byte[dataInputStream.readInt()];
        dataInputStream.readFully(compressedData);

        return new CacheChunkEntry(key, compressedData);
      }
    } catch (IOException e) {
      throw new IOException("Unable to read chunk: " + key, e);
    }

    return null;
  }

  public void write(ChunkCacheKey key, @Nullable CacheChunkEntry value) throws IOException {
    try (DataOutputStream dataOutputStream = this.regionFileCache.createOutputStream(key)) {
      dataOutputStream.writeInt(CACHE_VERSION);

      if (value != null) {
        dataOutputStream.writeBoolean(true);

        byte[] compressedData = value.compressedData();
        dataOutputStream.writeInt(compressedData.length);
        dataOutputStream.write(compressedData);
      } else {
        dataOutputStream.writeBoolean(false);
      }
    } catch (IOException e) {
      throw new IOException("Unable to write chunk: " + key, e);
    }
  }
}
