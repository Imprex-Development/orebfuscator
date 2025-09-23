package dev.imprex.orebfuscator.obfuscation;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import dev.imprex.orebfuscator.util.ChunkCacheKey;

public record CacheRequest(@NotNull ObfuscationRequest request, @NotNull ChunkCacheKey cacheKey, @NotNull byte[] hash) {

  public static final HashFunction HASH_FUNCTION = Hashing.murmur3_128();
  public static final int HASH_LENGTH = HASH_FUNCTION.bits() / Byte.SIZE;

  public CacheRequest(
      @NotNull ObfuscationRequest request,
      @NotNull ChunkCacheKey cacheKey,
      @NotNull byte[] hash) {
    this.request = Objects.requireNonNull(request);
    this.cacheKey = Objects.requireNonNull(cacheKey);
    this.hash = Objects.requireNonNull(hash);
  }

  public CompletableFuture<ObfuscationResponse> future() {
    return this.request().future();
  }

  public void complete(CacheResponse response) {
    this.request().future().complete(response.response());
  }
}
