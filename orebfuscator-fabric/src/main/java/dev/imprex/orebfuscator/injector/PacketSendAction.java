package dev.imprex.orebfuscator.injector;

import dev.imprex.orebfuscator.Orebfuscator;
import io.netty.channel.ChannelFutureListener;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;

public record PacketSendAction(Packet<?> packet, ChannelFutureListener listener, boolean flush,
                               CompletableFuture<Void> future, long enqueuedAt) implements Consumer<Connection> {

  public PacketSendAction(Packet<?> packet, ChannelFutureListener listener, boolean flush,
      CompletableFuture<Void> future) {
    this(packet, listener, flush, future, System.nanoTime());
  }

  public boolean isDone() {
    return future == null || future.isDone();
  }

  @Override
  public void accept(Connection connection) {
    connection.sendPacket(packet, listener, flush);
    Orebfuscator.injectorStatistics.packetDelayAny.add(System.nanoTime() - this.enqueuedAt);
  }
}
