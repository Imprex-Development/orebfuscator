package net.imprex.orebfuscator.cache;

import java.util.function.Consumer;

import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.obfuscation.ObfuscatedChunk;
import net.imprex.orebfuscator.obfuscation.Obfuscator;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ChunkCacheRequest {

	private final Obfuscator obfuscator;
	private final ChunkPosition key;
	private final byte[] hash;
	private final ChunkStruct chunkStruct;

	public ChunkCacheRequest(Obfuscator obfuscator, ChunkPosition key, byte[] hash, ChunkStruct chunkStruct) {
		this.obfuscator = obfuscator;
		this.key = key;
		this.hash = hash;
		this.chunkStruct = chunkStruct;
	}

	public void obfuscate(Consumer<ObfuscatedChunk> consumer) {
		this.obfuscator.obfuscate(this, consumer);
	}

	public ChunkPosition getKey() {
		return key;
	}

	public byte[] getHash() {
		return hash;
	}

	public ChunkStruct getChunkStruct() {
		return chunkStruct;
	}
}
