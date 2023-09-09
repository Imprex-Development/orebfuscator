package net.imprex.orebfuscator.compatibility;

import java.util.concurrent.CompletableFuture;

import org.bukkit.World;

import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.util.ChunkPosition;

public interface CompatibilityLayer {

	CompatibilityScheduler getScheduler();

	CompletableFuture<ReadOnlyChunk[]> getNeighboringChunks(World world, ChunkPosition position);
}
