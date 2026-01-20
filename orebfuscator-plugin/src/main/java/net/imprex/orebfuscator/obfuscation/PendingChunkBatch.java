package net.imprex.orebfuscator.obfuscation;

import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.events.PacketEvent;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.statistics.InjectorStatistics;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PendingChunkBatch {

  private final long enqueuedAt = System.nanoTime();

  private final InjectorStatistics statistics;
  private final AsynchronousManager asynchronousManager;

  private final AtomicBoolean finished = new AtomicBoolean(false);
  private final PacketEvent startPacket;

  private final List<CompletableFuture<Void>> pendingFutures = new ArrayList<>();

  public PendingChunkBatch(InjectorStatistics statistics, AsynchronousManager asynchronousManager,
      PacketEvent startPacket) {
    this.statistics = statistics;
    this.asynchronousManager = asynchronousManager;

    this.startPacket = startPacket;
    startPacket.getAsyncMarker().incrementProcessingDelay();
  }

  public void addChunk(CompletableFuture<Void> future) {
    if (!this.finished.get()) {
      this.pendingFutures.add(future);
    }
  }

  public void finish() {
    if (this.finished.compareAndSet(false, true)) {
      var futures = this.pendingFutures.toArray(CompletableFuture[]::new);

      CompletableFuture.allOf(futures).whenComplete((v, throwable) -> {
        if (throwable != null) {
          OfcLogger.error("An error occurred while processing a chunk batch", throwable);
        }

        // only delay/signal start packet as any packet after has to wait anyways and that way we
        // only take up a single processing slot in ProtocolLib's async filter manager per batch
        this.asynchronousManager.signalPacketTransmission(this.startPacket);

        statistics.packetDelayChunk.add(System.nanoTime() - this.enqueuedAt);
      });
    }
  }
}
