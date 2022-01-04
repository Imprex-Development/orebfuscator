package net.imprex.orebfuscator.obfuscation;

import java.util.Set;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkDirection;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ObfuscationTask {

	public static ObfuscationTask fromRequest(ObfuscationRequest request) {
		ObfuscationTask task = new ObfuscationTask(request);

		ChunkPosition position = request.getPosition();
		for (ChunkDirection direction : ChunkDirection.values()) {
			int chunkX = position.getX() + direction.getOffsetX();
			int chunkZ = position.getZ() + direction.getOffsetZ();
			ReadOnlyChunk chunk = NmsInstance.getReadOnlyChunk(position.getWorld(), chunkX, chunkZ);
			task.neighboringChunks[direction.ordinal()] = chunk;
		}

		return task;
	}

	private final ObfuscationRequest request;
	private final ReadOnlyChunk[] neighboringChunks = new ReadOnlyChunk[4];

	private ObfuscationTask(ObfuscationRequest request) {
		this.request = request;
	}

	public ChunkStruct getChunkStruct() {
		return this.request.getChunkStruct();
	}

	public void complete(byte[] data, Set<BlockPos> blockEntities, Set<BlockPos> proximityBlocks) {
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
