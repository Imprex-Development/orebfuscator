package net.imprex.orebfuscator.compatibility;

import java.util.concurrent.CompletableFuture;

import org.bukkit.World;

import dev.imprex.orebfuscator.util.ChunkCacheKey;
import dev.imprex.orebfuscator.interop.ChunkAccessor;

public interface CompatibilityLayer {

  boolean isGameThread();

  CompatibilityScheduler getScheduler();

  CompletableFuture<ChunkAccessor[]> getNeighboringChunks(World world, ChunkCacheKey key);
}
