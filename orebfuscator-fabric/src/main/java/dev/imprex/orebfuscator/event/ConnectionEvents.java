package dev.imprex.orebfuscator.event;

import java.util.concurrent.CompletableFuture;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class ConnectionEvents {

  public static final Event<Send> SEND =
      EventFactory.createArrayBacked(Send.class, callbacks -> (connection, packet) -> {
        for (Send callback : callbacks) {
          var future = callback.onPacketSend(connection, packet);
          if (future != null) {
            return future;
          }
        }

        return null;
      });

  public static final Event<Receive> RECEIVE =
      EventFactory.createArrayBacked(Receive.class, callbacks -> (connection, packet, packetListener) -> {
        for (Receive callback : callbacks) {
          callback.onPacketReceive(connection, packet, packetListener);
        }
      });

  private ConnectionEvents() {
  }

  @FunctionalInterface
  public interface Send {
    @Nullable CompletableFuture<Void> onPacketSend(Connection connection, Packet<?> packet);
  }


  @FunctionalInterface
  public interface Receive {
    void onPacketReceive(Connection connection, Packet<?> packet, PacketListener packetListener);
  }
}
