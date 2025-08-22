package net.imprex.orebfuscator.obfuscation;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.World;

import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.ChunkDirection;
import dev.imprex.orebfuscator.util.ChunkCacheKey;
import net.imprex.orebfuscator.OrebfuscatorCompatibility;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;

public class ObfuscationTask {

	public static CompletableFuture<ObfuscationTask> fromRequest(ObfuscationRequest request) {
		World world = request.getChunkStruct().world;
		ChunkCacheKey position = request.getPosition();

		return OrebfuscatorCompatibility.getNeighboringChunks(world, position)
			.thenApply(chunks -> new ObfuscationTask(request, chunks));
	}

	private final ObfuscationRequest request;
	private final ReadOnlyChunk[] neighboringChunks;

	private ObfuscationTask(ObfuscationRequest request, ReadOnlyChunk[] neighboringChunks) {
		if (neighboringChunks == null || neighboringChunks.length != 4) {
			throw new IllegalArgumentException("neighboringChunks missing or invalid length");
		}

		this.request = request;
		this.neighboringChunks = neighboringChunks;
	}

	public ChunkStruct getChunkStruct() {
		return this.request.getChunkStruct();
	}

	public void complete(byte[] data, Set<BlockPos> blockEntities, List<BlockPos> proximityBlocks) {
		this.request.complete(this.request.createResult(data, blockEntities, proximityBlocks));
	}

	public void completeExceptionally(Throwable throwable) {
		this.request.completeExceptionally(throwable);
	}

	public int getBlockState(int x, int y, int z) {
		ChunkDirection direction = ChunkDirection.fromPosition(request.getPosition(), x, z);
		return this.neighboringChunks[direction.ordinal()].getBlockState(x, y, z);
	}
}
