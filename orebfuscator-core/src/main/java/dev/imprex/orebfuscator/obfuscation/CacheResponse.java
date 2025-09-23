package dev.imprex.orebfuscator.obfuscation;

import org.jetbrains.annotations.NotNull;
import dev.imprex.orebfuscator.util.ChunkCacheKey;

public record CacheResponse(@NotNull ObfuscationResponse response, @NotNull ChunkCacheKey cacheKey, @NotNull byte[] hash) {
}
