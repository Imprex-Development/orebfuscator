package net.imprex.orebfuscator.obfuscation;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import dev.imprex.orebfuscator.config.OrebfuscatorConfig;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.ChunkCacheKey;
import net.imprex.orebfuscator.iterop.BukkitChunkPacketAccessor;

public class ObfuscationRequest {

  private static final HashFunction HASH_FUNCTION = Hashing.murmur3_128();
  private static final byte[] EMPTY_HASH = new byte[0];

  public static final int HASH_LENGTH = HASH_FUNCTION.bits() / Byte.SIZE;

  private static final byte[] hash(byte[] systemHash, byte[] data) {
    return HASH_FUNCTION.newHasher().putBytes(systemHash).putBytes(data).hash().asBytes();
  }

  public static ObfuscationRequest fromChunk(BukkitChunkPacketAccessor packet, OrebfuscatorConfig config,
      ObfuscationTaskDispatcher dispatcher) {
    byte[] hash = config.cache().enabled() ? hash(config.systemHash(), packet.data()) : EMPTY_HASH;
    return new ObfuscationRequest(dispatcher, packet, hash);
  }

  private final CompletableFuture<ObfuscationResult> future = new CompletableFuture<>();

  private final ObfuscationTaskDispatcher dispatcher;
  private final BukkitChunkPacketAccessor packet;

  private final ChunkCacheKey chunkCacheKey;
  private final byte[] chunkHash;

  private ObfuscationRequest(ObfuscationTaskDispatcher dispatcher, BukkitChunkPacketAccessor packet, byte[] chunkHash) {
    this.dispatcher = dispatcher;
    this.packet = packet;

    this.chunkCacheKey = new ChunkCacheKey(packet.world().getName(), packet.chunkX(), packet.chunkZ());
    this.chunkHash = chunkHash;
  }

  public CompletableFuture<ObfuscationResult> getFuture() {
    return future;
  }

  public ChunkCacheKey getCacheKey() {
    return chunkCacheKey;
  }

  public byte[] getChunkHash() {
    return chunkHash;
  }

  public BukkitChunkPacketAccessor getPacket() {
    return packet;
  }

  public CompletableFuture<ObfuscationResult> submitForObfuscation() {
    this.dispatcher.submitRequest(this);
    return this.future;
  }

  public ObfuscationResult createResult(byte[] data, Set<BlockPos> blockEntities, List<BlockPos> proximityBlocks) {
    return new ObfuscationResult(this.chunkCacheKey, this.chunkHash, data, blockEntities, proximityBlocks);
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
