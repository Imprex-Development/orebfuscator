package net.imprex.orebfuscator.compatibility.paper;

import java.util.concurrent.CompletableFuture;

import org.bukkit.World;

import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.util.ChunkDirection;
import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.compatibility.CompatibilityLayer;

public abstract class AbstractPaperCompatibilityLayer implements CompatibilityLayer {

	@Override
	public CompletableFuture<ChunkAccessor[]> getNeighboringChunks(World world, int chunkX, int chunkZ) {
		CompletableFuture<?>[] futures = new CompletableFuture<?>[4];
		ChunkAccessor[] neighboringChunks = new ChunkAccessor[4];

		for (ChunkDirection direction : ChunkDirection.values()) {
			int x = chunkX + direction.getOffsetX();
			int z = chunkX + direction.getOffsetZ();
			int index = direction.ordinal();

			futures[index] = world.getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
				neighboringChunks[index] = OrebfuscatorNms.getReadOnlyChunk(world, x, z);
			});
		}

		return CompletableFuture.allOf(futures).thenApply(v -> neighboringChunks);
	}
}
