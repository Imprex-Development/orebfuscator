package net.imprex.orebfuscator.compatibility.paper;

import java.util.concurrent.CompletableFuture;

import org.bukkit.World;

import dev.imprex.orebfuscator.util.ChunkCacheKey;
import dev.imprex.orebfuscator.util.ChunkDirection;
import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.compatibility.CompatibilityLayer;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;

public abstract class AbstractPaperCompatibilityLayer implements CompatibilityLayer {

	@Override
	public CompletableFuture<ReadOnlyChunk[]> getNeighboringChunks(World world, ChunkCacheKey key) {
		CompletableFuture<?>[] futures = new CompletableFuture<?>[4];
		ReadOnlyChunk[] neighboringChunks = new ReadOnlyChunk[4];

		for (ChunkDirection direction : ChunkDirection.values()) {
			int chunkX = key.x() + direction.getOffsetX();
			int chunkZ = key.z() + direction.getOffsetZ();
			int index = direction.ordinal();

			futures[index] = world.getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
				neighboringChunks[index] = OrebfuscatorNms.getReadOnlyChunk(world, chunkX, chunkZ);
			});
		}

		return CompletableFuture.allOf(futures).thenApply(v -> neighboringChunks);
	}
}
