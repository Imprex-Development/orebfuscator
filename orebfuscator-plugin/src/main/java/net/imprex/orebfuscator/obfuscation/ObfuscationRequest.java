package net.imprex.orebfuscator.obfuscation;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ObfuscationRequest {

	private static final HashFunction HASH_FUNCTION = Hashing.murmur3_128();
	private static final byte[] EMPTY_HASH = new byte[0];

	public static final int HASH_LENGTH = HASH_FUNCTION.bits() / Byte.SIZE;

	private static final byte[] hash(byte[] systemHash, byte[] data) {
		return HASH_FUNCTION.newHasher().putBytes(systemHash).putBytes(data).hash().asBytes();
	}

	public static ObfuscationRequest fromChunk(ChunkStruct struct, OrebfuscatorConfig config,
			ObfuscationTaskDispatcher dispatcher) {
		ChunkPosition position = new ChunkPosition(struct.world, struct.chunkX, struct.chunkZ);
		byte[] hash = config.cache().enabled() ? hash(config.systemHash(), struct.data) : EMPTY_HASH;
		return new ObfuscationRequest(dispatcher, position, hash, struct);
	}

	private final CompletableFuture<ObfuscationResult> future = new CompletableFuture<>();

	private final ObfuscationTaskDispatcher dispatcher;
	private final ChunkPosition position;
	private final byte[] chunkHash;
	private final ChunkStruct chunkStruct;

	private ObfuscationRequest(ObfuscationTaskDispatcher dispatcher, ChunkPosition position, byte[] chunkHash,
			ChunkStruct chunkStruct) {
		this.dispatcher = dispatcher;
		this.position = position;
		this.chunkHash = chunkHash;
		this.chunkStruct = chunkStruct;
	}

	public CompletableFuture<ObfuscationResult> getFuture() {
		return future;
	}

	public ChunkPosition getPosition() {
		return position;
	}

	public byte[] getChunkHash() {
		return chunkHash;
	}

	public ChunkStruct getChunkStruct() {
		return chunkStruct;
	}

	public boolean isValid(ObfuscationResult result) {
		return result != null && Arrays.equals(result.getHash(), this.chunkHash);
	}

	public CompletableFuture<ObfuscationResult> submitForObfuscation() {
		this.dispatcher.submitRequest(this);
		return this.future;
	}

	public ObfuscationResult createResult(byte[] data, Set<BlockPos> blockEntities, List<BlockPos> proximityBlocks) {
		return new ObfuscationResult(this.position, this.chunkHash, data, blockEntities, proximityBlocks);
	}

	public CompletableFuture<ObfuscationResult> complete(ObfuscationResult result) {
		this.future.complete(result);
		return this.future;
	}

	public CompletableFuture<ObfuscationResult> completeExceptionally(Throwable throwable) {
		this.future.completeExceptionally(throwable);
		return this.future;
	}
}
