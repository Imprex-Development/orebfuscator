package net.imprex.orebfuscator.obfuscation;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.World;

import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.ChunkCacheKey;
import dev.imprex.orebfuscator.util.ChunkDirection;
import net.imprex.orebfuscator.OrebfuscatorCompatibility;
import net.imprex.orebfuscator.iterop.BukkitChunkPacketAccessor;

public class ObfuscationTask {

	public static CompletableFuture<ObfuscationTask> fromRequest(ObfuscationRequest request) {
		World world = request.getPacket().worldAccessor.world;
		ChunkCacheKey key = request.getCacheKey();

		return OrebfuscatorCompatibility.getNeighboringChunks(world, key.x(), key.z())
			.thenApply(chunks -> new ObfuscationTask(request, chunks));
	}

	private final ObfuscationRequest request;
	private final ChunkAccessor[] neighboringChunks;

	private ObfuscationTask(ObfuscationRequest request, ChunkAccessor[] neighboringChunks) {
		if (neighboringChunks == null || neighboringChunks.length != 4) {
			throw new IllegalArgumentException("neighboringChunks missing or invalid length");
		}

		this.request = request;
		this.neighboringChunks = neighboringChunks;
	}

	public BukkitChunkPacketAccessor getPacket() {
		return this.request.getPacket();
	}

	public void complete(byte[] data, Set<BlockPos> blockEntities, List<BlockPos> proximityBlocks) {
		this.request.complete(this.request.createResult(data, blockEntities, proximityBlocks));
	}

	public void completeExceptionally(Throwable throwable) {
		this.request.completeExceptionally(throwable);
	}

	public int getBlockState(int x, int y, int z) {
		ChunkDirection direction = ChunkDirection.fromPosition(request.getCacheKey(), x, z);
		return this.neighboringChunks[direction.ordinal()].getBlockState(x, y, z);
	}
}
