package dev.imprex.orebfuscator.injector;

import dev.imprex.orebfuscator.Orebfuscator;
import io.netty.channel.ChannelFutureListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import org.jspecify.annotations.Nullable;

public class ChunkBatchAction implements PendingAction {

  private final long enqueuedAt = System.nanoTime();

  private final AtomicBoolean finished = new AtomicBoolean(false);
  private final List<PacketSendAction> actions = new ArrayList<>();

  public void addChunk(Packet<?> packet, @Nullable ChannelFutureListener listener, CompletableFuture<Void> future) {
    if (!this.finished.get()) {
      this.actions.add(new PacketSendAction(packet, listener, false, future));
    }
  }

  public void finish() {
    this.finished.compareAndSet(false, true);
  }

  @Override
  public boolean isComplete() {
    return this.finished.get() && this.actions.stream().allMatch(PacketSendAction::isDone);
  }

  @Override
  public void accept(Connection connection) {
    connection.sendPacket(ClientboundChunkBatchStartPacket.INSTANCE, null, false);

    for (var action : this.actions) {
      action.accept(connection);
    }

    connection.sendPacket(new ClientboundChunkBatchFinishedPacket(this.actions.size()), null, true);
    Orebfuscator.injectorStatistics.packetDelayChunk.add(System.nanoTime() - this.enqueuedAt);
  }
}
