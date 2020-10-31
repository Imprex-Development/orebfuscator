package net.imprex.orebfuscator.cache;

import java.io.IOException;

import net.imprex.orebfuscator.obfuscation.ObfuscatedChunk;
import net.imprex.orebfuscator.util.ChunkPosition;

public class WriteTask implements SerializerTask {

	private final ChunkPosition position;
	final ObfuscatedChunk chunk;

	public WriteTask(ChunkPosition position, ObfuscatedChunk chunk) {
		this.position = position;
		this.chunk = chunk;
	}

	@Override
	public int estimatedHeapSize() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void run() {
		try {
			ChunkSerializer.write(this.position, this.chunk);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
