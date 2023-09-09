package net.imprex.orebfuscator.compatibility.folia;

import java.util.concurrent.CompletableFuture;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.compatibility.CompatibilityLayer;
import net.imprex.orebfuscator.compatibility.CompatibilityScheduler;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.util.ChunkDirection;
import net.imprex.orebfuscator.util.ChunkPosition;

public class FoliaCompatibilityLayer implements CompatibilityLayer {

	private final FoliaScheduler scheduler;

	public FoliaCompatibilityLayer(Plugin plugin, Config config) {
		this.scheduler = new FoliaScheduler(plugin);
	}

	@Override
	public boolean isMojangMapped() {
		return true;
	}

	@Override
	public CompatibilityScheduler getScheduler() {
		return this.scheduler;
	}

	@Override
	public CompletableFuture<ReadOnlyChunk[]> getNeighboringChunks(World world, ChunkPosition position) {
		CompletableFuture<?>[] futures = new CompletableFuture<?>[4];
		ReadOnlyChunk[] neighboringChunks = new ReadOnlyChunk[4];

		for (ChunkDirection direction : ChunkDirection.values()) {
			int chunkX = position.x + direction.getOffsetX();
			int chunkZ = position.z + direction.getOffsetZ();
			int index = direction.ordinal();

			futures[index] = world.getChunkAtAsync(chunkX, chunkZ).thenAccept(chunk -> {
				neighboringChunks[index] = OrebfuscatorNms.getReadOnlyChunk(world, chunkX, chunkZ);
			});
		}

		return CompletableFuture.allOf(futures).thenApply(v -> neighboringChunks);
	}	
}
