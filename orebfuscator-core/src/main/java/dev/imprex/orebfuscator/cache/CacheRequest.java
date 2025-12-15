package dev.imprex.orebfuscator.cache;

import java.util.Objects;
import org.jspecify.annotations.NullMarked;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import dev.imprex.orebfuscator.util.ChunkCacheKey;

@NullMarked
public record CacheRequest(ChunkCacheKey cacheKey, byte[] hash) {

  public static final HashFunction HASH_FUNCTION = Hashing.murmur3_128();
  public static final int HASH_LENGTH = HASH_FUNCTION.bits() / Byte.SIZE;

  public CacheRequest {
    Objects.requireNonNull(cacheKey);
    Objects.requireNonNull(hash);
  }

  @Override
  public String toString() {
    return "CacheRequest [cacheKey=" + cacheKey + "]";
  }
}
