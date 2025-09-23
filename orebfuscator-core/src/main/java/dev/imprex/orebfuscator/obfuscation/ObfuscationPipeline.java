package dev.imprex.orebfuscator.obfuscation;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import dev.imprex.orebfuscator.cache.ObfuscationCache;
import dev.imprex.orebfuscator.config.api.Config;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.interop.ChunkPacketAccessor;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.statistics.OrebfuscatorStatistics;
import dev.imprex.orebfuscator.util.ChunkCacheKey;

public class ObfuscationPipeline {

  private static final ChunkAccessor[] EMPTY_NEIGHBORS = new ChunkAccessor[] {
      ChunkAccessor.EMPTY, ChunkAccessor.EMPTY, ChunkAccessor.EMPTY, ChunkAccessor.EMPTY
  };

  private static final HashFunction HASH_FUNCTION = Hashing.murmur3_128();
  public static final int HASH_LENGTH = HASH_FUNCTION.bits() / Byte.SIZE;

  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(8, r -> {
    Thread thread = new ObfuscationThread(OrebfuscatorCore.THREAD_GROUP, r, "ofc-test-thread");
    return thread;
  });

  private final Config config;
  private final ObfuscationCache cache;
  private final ObfuscationProcessor processor;
  private final OrebfuscatorStatistics statistics;

  public ObfuscationPipeline(OrebfuscatorCore orebfuscator) {
    this.config = orebfuscator.config();
    this.cache = orebfuscator.cache();
    this.processor = orebfuscator.obfuscationProcessor();
    this.statistics = orebfuscator.statistics();
  }

  public @NotNull CompletableFuture<ObfuscationResponse> submit(@NotNull ChunkPacketAccessor packetAccessor, @Nullable ChunkAccessor[] neighborChunks) {
    Objects.requireNonNull(packetAccessor);

    if (neighborChunks != null) {
      submitRequest(new ObfuscationRequest(packetAccessor, neighborChunks));
    }

    int chunkX = packetAccessor.chunkX();
    int chunkZ = packetAccessor.chunkZ();

    var timer = statistics.injector.packetDelayNeighbors.start();
    return packetAccessor.world().getNeighboringChunks(chunkX, chunkZ).exceptionally(throwable -> {
      OfcLogger.error(String.format("Can't get neighboring chunks for (%d, %d)", chunkX, chunkZ), throwable);
      return EMPTY_NEIGHBORS;
    }).thenCompose(result -> {
      timer.stop();
      var neighbors = result != null ? result : EMPTY_NEIGHBORS;
      return submitRequest(new ObfuscationRequest(packetAccessor, neighbors));
    });
  }

  public CompletableFuture<ObfuscationResponse> submitRequest(@NotNull CacheRequest request) {
    if (!(Thread.currentThread() instanceof ObfuscationThread)) {
      executorService.submit(() -> submitRequest(request));
      return request.future();
    }

    this.processor.process(request.request());

    return request.future();
  }

  private @NotNull CompletableFuture<ObfuscationResponse> submitRequest(@NotNull ObfuscationRequest request) {
    Objects.requireNonNull(request);

    var timer = statistics.injector.packetDelayExecutor.start();
    executorService.submit(() -> {
      timer.stop();
      scheduleRequest(request);
    });

    return request.future();
  }

  private void scheduleRequest(@NotNull ObfuscationRequest request) {
    if (config.cache().enabled()) {
      ChunkCacheKey cacheKey = new ChunkCacheKey(request.packetAccessor());

      byte[] hash = HASH_FUNCTION.newHasher()
        .putBytes(config.systemHash())
        .putBytes(request.packetAccessor().data())
        .hash()
        .asBytes();

      var cacheRequest = new CacheRequest(request, cacheKey, hash);
      this.cache.get(cacheRequest);
    } else {
      this.processor.process(request);
    }
  }

  private static class ObfuscationThread extends Thread {
    public ObfuscationThread(ThreadGroup group, Runnable target, String name) {
      super(group, target, name);
    }
  }
}
