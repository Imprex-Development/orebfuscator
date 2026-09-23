package dev.imprex.orebfuscator.injector;

import dev.imprex.orebfuscator.Orebfuscator;
import dev.imprex.orebfuscator.PermissionRequirements;
import dev.imprex.orebfuscator.config.api.ProximityConfig;
import dev.imprex.orebfuscator.event.ConnectionEvents;
import dev.imprex.orebfuscator.iterop.FabricChunkPacketAccessor;
import dev.imprex.orebfuscator.iterop.FabricPlayerAccessor;
import dev.imprex.orebfuscator.obfuscation.ObfuscationPipeline;
import dev.imprex.orebfuscator.statistics.InjectorStatistics;

import java.util.concurrent.CompletableFuture;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

public class OrebfuscatorInjector {

  private final ObfuscationPipeline pipeline;
  private final InjectorStatistics statistics;

  public OrebfuscatorInjector(Orebfuscator orebfuscator) {
    this.pipeline = orebfuscator.obfuscationPipeline();
    this.statistics = orebfuscator.statistics().injector;

    ConnectionEvents.SEND.register(this::onPacketSend);
    ConnectionEvents.RECEIVE.register(this::onPacketReceive);
  }

  private void onPacketReceive(Connection connection, Packet<?> packet, PacketListener packetListener) {
    if (packet instanceof ServerboundChunkBatchReceivedPacket(float desiredChunksPerTick)) {
      statistics.injectorBatchSize.add(Math.round(desiredChunksPerTick));
    }
  }

  private CompletableFuture<Void> onPacketSend(Connection connection, Packet<?> packet) {
    var timer = statistics.injectorDelaySync.start();
    try {
      if (!(connection.getPacketListener() instanceof ServerGamePacketListenerImpl listener)) {
        return null;
      }

      var player = FabricPlayerAccessor.tryGet(listener.player);
      if (player == null) {
        return null;
      }

      if (packet instanceof ClientboundLevelChunkWithLightPacket chunkPacket) {
        return onChunkSend(player, chunkPacket);
      } else if (packet instanceof ClientboundForgetLevelChunkPacket forgetPacket) {
        onChunkForgetSend(player, forgetPacket);
      }

      return null;
    } finally {
      timer.stop();
    }
  }

  private CompletableFuture<Void> onChunkSend(FabricPlayerAccessor player,
      ClientboundLevelChunkWithLightPacket packet) {
    if (!player.isAlive()) {
      return null;
    }

    var world = player.world();
    if (player.hasPermission(PermissionRequirements.BYPASS) || !world.config().needsObfuscation()) {
      return null;
    }

    var packetAccessor = new FabricChunkPacketAccessor(packet);

    var neighboringChunks = world.getNeighboringChunksNow(packetAccessor.chunkX(), packetAccessor.chunkZ());

    return pipeline.request(world, player, packetAccessor, neighboringChunks).toCompletableFuture();
  }

  private void onChunkForgetSend(FabricPlayerAccessor player, ClientboundForgetLevelChunkPacket packet) {
    if (player.hasPermission(PermissionRequirements.BYPASS)) {
      return;
    }

    ProximityConfig proximityConfig = player.world().config().proximity();
    if (proximityConfig == null || !proximityConfig.isEnabled()) {
      return;
    }

    player.orebfuscatorPlayer().removeChunk(player.world(), packet.pos().x(), packet.pos().z());
  }
}
