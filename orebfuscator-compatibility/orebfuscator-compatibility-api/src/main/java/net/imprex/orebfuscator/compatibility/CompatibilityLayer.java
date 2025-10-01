package net.imprex.orebfuscator.compatibility;

import java.util.concurrent.CompletableFuture;

import org.bukkit.World;

import dev.imprex.orebfuscator.util.ChunkCacheKey;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;

public interface CompatibilityLayer {

  boolean isGameThread();

  CompatibilityScheduler getScheduler();

  CompletableFuture<ReadOnlyChunk[]> getNeighboringChunks(World world, ChunkCacheKey key);
}
