package net.imprex.orebfuscator.compatibility;

import java.util.concurrent.CompletableFuture;

import org.bukkit.World;

import dev.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;

public interface CompatibilityLayer {

  boolean isGameThread();

  CompatibilityScheduler getScheduler();

  CompletableFuture<ReadOnlyChunk[]> getNeighboringChunks(World world, ChunkPosition position);
}
