package dev.imprex.orebfuscator.cache;

import dev.imprex.orebfuscator.util.ChunkCacheKey;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ChunkSerializer {

  private static final int CACHE_VERSION = 3;

  private final AbstractRegionFileCache<?> regionFileCache;

  public ChunkSerializer(AbstractRegionFileCache<?> regionFileCache) {
    this.regionFileCache = regionFileCache;
  }

  @Nullable
  public ChunkCacheEntry read(ChunkCacheKey key) throws IOException {
    try (DataInputStream dataInputStream = this.regionFileCache.createInputStream(key)) {
      // check if cache entry has right version and if chunk is present
      if (dataInputStream == null || dataInputStream.readInt() != CACHE_VERSION || !dataInputStream.readBoolean()) {
        return null;
      }

      byte[] compressedData = new byte[dataInputStream.readInt()];
      dataInputStream.readFully(compressedData);

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
      } else {
        dataOutputStream.writeBoolean(false);
      }
    } catch (IOException e) {
      throw new IOException("Unable to write chunk: " + key, e);
    }
  }
}
