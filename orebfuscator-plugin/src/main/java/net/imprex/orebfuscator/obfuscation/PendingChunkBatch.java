package net.imprex.orebfuscator.obfuscation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NullMarked;
import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.events.PacketEvent;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.statistics.InjectorStatistics;

@NullMarked
public class PendingChunkBatch {

  private final InjectorStatistics statistics;
  private final long enqueuedAt = System.nanoTime();

  private final AsynchronousManager asynchronousManager;
  private final AtomicBoolean finished = new AtomicBoolean(false);
  private final PacketEvent event;

  private final List<CompletableFuture<Void>> pendingFutures = new ArrayList<>();

  public PendingChunkBatch(InjectorStatistics statistics, AsynchronousManager asynchronousManager, PacketEvent event) {
    this.statistics = statistics;
    this.asynchronousManager = asynchronousManager;

    this.event = event;
    event.getAsyncMarker().incrementProcessingDelay();
  }

  public void addChunk(PacketEvent event, CompletableFuture<Void> future) {
    if (!this.finished.get()) {
      this.pendingFutures.add(future);
    }
  }

  public void finish(PacketEvent event) {
    if (this.finished.compareAndSet(false, true)) {
      var futures = this.pendingFutures.toArray(CompletableFuture[]::new);

      CompletableFuture.allOf(futures).whenComplete((v, throwable) -> {
        if (throwable != null) {
          OfcLogger.error("An error occurred while processing a chunk batch", throwable);
        }

        // only delay/signal start packet as any packet after has to wait anyways and that way we
        // only take up a single processing slot in ProtocolLib's async filter manager per batch
        this.asynchronousManager.signalPacketTransmission(this.event);

        statistics.packetDelayChunk.add(System.nanoTime() - this.enqueuedAt);
      });
    }
  }
}
