package net.imprex.orebfuscator.cache;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import net.imprex.orebfuscator.obfuscation.ObfuscatedChunk;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ReadTask implements SerializerTask {

	private final ChunkPosition position;
	final CompletableFuture<ObfuscatedChunk> future;

	public ReadTask(ChunkPosition position, CompletableFuture<ObfuscatedChunk> future) {
		this.position = position;
		this.future = future;
	}

	@Override
	public int estimatedHeapSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void run() {
		if (!this.future.isDone()) {
			try {
				this.future.complete(ChunkSerializer.read(this.position));
			} catch (IOException e) {
				this.future.completeExceptionally(e);
				e.printStackTrace();
			}
		}
	}

	public void completeEmpty() {
		this.future.complete(null);
	}
}
