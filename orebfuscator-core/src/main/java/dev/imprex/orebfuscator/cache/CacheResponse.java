package dev.imprex.orebfuscator.cache;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;

@NullMarked
public sealed interface CacheResponse permits CacheResponse.Success, CacheResponse.Failure {

  public static CacheResponse success(ChunkCacheEntry entry) {
    return new Success(entry);
  }
  
  record Success(ChunkCacheEntry entry) implements CacheResponse {
    public Success {
      Objects.requireNonNull(entry, "entry");
    }
  }

  enum Failure implements CacheResponse {
    NOT_FOUND, MEMORY_INVALID, DISK_INVALID;
  }
}
