package dev.imprex.orebfuscator.obfuscation;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import dev.imprex.orebfuscator.cache.CacheRequest;
import dev.imprex.orebfuscator.cache.ObfuscationCache;
import dev.imprex.orebfuscator.config.api.AdvancedConfig;
import dev.imprex.orebfuscator.config.api.Config;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.interop.ChunkPacketAccessor;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.interop.PlayerAccessor;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.statistics.OrebfuscatorStatistics;
import dev.imprex.orebfuscator.util.ChunkCacheKey;
import dev.imprex.orebfuscator.util.concurrent.OrebfuscatorExecutor;

@NullMarked
public class ObfuscationPipeline {

  private final Config config;
  private final ObfuscationCache cache;
  private final ObfuscationProcessor processor;
  private final OrebfuscatorStatistics statistics;
  private final OrebfuscatorExecutor executor;

  public ObfuscationPipeline(OrebfuscatorCore orebfuscator) {
    this.config = orebfuscator.config();
    this.cache = orebfuscator.cache();
    this.processor = orebfuscator.obfuscationProcessor();
    this.statistics = orebfuscator.statistics();
    this.executor = orebfuscator.executor();
  }

  public CompletionStage<Void> request(
      WorldAccessor world,
      PlayerAccessor player,
      ChunkPacketAccessor packet,
      @Nullable ChunkAccessor @Nullable[] neighborChunks) {
    var timer = statistics.injector.pipelineDelayTotal.start();
    return timer.wrap(requestInternal(world, player, packet, neighborChunks));
  }

  private CompletionStage<Void> requestInternal(
      WorldAccessor world,
      PlayerAccessor player,
      ChunkPacketAccessor packet,
      @Nullable ChunkAccessor @Nullable[] neighborChunks) {

    final var request = new ObfuscationRequest(world, player, packet, neighborChunks);

    final var neighborTimer = statistics.injector.pipelineDelayNeighbors.start();
//    final var neighborFuture = neighborTimer.wrap(world.getNeighboringChunks(request));

    final CacheRequest cacheRequest;
    final CompletionStage<Optional<ObfuscationResponse>> cacheFuture;

    if (config.cache().enabled()) {
      ChunkCacheKey cacheKey = new ChunkCacheKey(request);

      byte[] hash = CacheRequest.HASH_FUNCTION.newHasher()
          .putBytes(config.systemHash())
          .putBytes(request.packet().data())
          .hash()
          .asBytes();

      cacheRequest = new CacheRequest(cacheKey, hash);
 
      var cacheTimer = statistics.injector.pipelineDelayCache.start();
      cacheFuture = cacheTimer.wrap(this.cache.get(cacheRequest));
    } else {
      cacheRequest = null;
      cacheFuture = CompletableFuture.completedStage(Optional.empty());
    }

    var future = cacheFuture.thenComposeAsync(optional -> {
      if (optional.isPresent()) {
        return CompletableFuture.completedStage(optional.get());
      } else {
        return neighborTimer.wrap(world.getNeighboringChunks(request))
            .handleAsync((neighbors, throwable) -> {
              if (throwable != null) {
                OfcLogger.error("Can't get neighboring chunks for (%d, %d)".formatted(packet.chunkX(), packet.chunkZ()),
                    throwable);
                return request;
              } else if (neighbors == null) {
                return request;
              }

              long missingChunks = Arrays.stream(neighbors).filter(n -> n == ChunkAccessor.EMPTY).count();
              statistics.obfuscation.missingNeighboringChunks.add(missingChunks);

              return request.withNeighbors(neighbors);
            }, this.executor)
            .thenApply(this.processor::process)
            .thenApply(response -> {
              if (config.cache().enabled() && cacheRequest != null) {
                cache.add(cacheRequest, response);
              }
              return response;
            });
      }
    }, this.executor);

    AdvancedConfig advancedConfig = config.advanced();
    if (advancedConfig.hasObfuscationTimeout()) {
      future = future
          .toCompletableFuture()
          .orTimeout(advancedConfig.obfuscationTimeout(), TimeUnit.MILLISECONDS);
    }
    
    return future.<Void>thenApplyAsync(response -> {
      this.postProcess(request, response);
      return null;
    }, this.executor).exceptionallyAsync(throwable -> {
      this.handleExceptions(request, throwable);
      return null;
    }, this.executor);
  }

  private void postProcess(ObfuscationRequest request, ObfuscationResponse response) {
    var packet = request.packet();

    statistics.obfuscation.originalChunkSize.add(packet.data().length);
    statistics.obfuscation.obfuscatedChunkSize.add(response.data().length);

    packet.update(response);

    var player = request.player().orebfuscatorPlayer();
    player.addChunk(request.world(), packet.chunkX(), packet.chunkZ(), response.proximityBlocks());
  }

  private void handleExceptions(ObfuscationRequest request, Throwable throwable) {
    var packet = request.packet();
    
    if (throwable instanceof CompletionException && throwable.getCause() != null) {
      throwable = throwable.getCause();
    }

    if (throwable instanceof TimeoutException) {
      OfcLogger.warn("Obfuscation for chunk[world=%s, x=%d, z=%d] timed out".formatted(request.world().getName(),
          packet.chunkX(), packet.chunkZ()));
    } else {
      OfcLogger.error("An error occurred while obfuscating chunk[world=%s, x=%d, z=%d]"
          .formatted(request.world().getName(), packet.chunkX(), packet.chunkZ()), throwable);
    }
  }
}
