package net.imprex.orebfuscator.obfuscation;

import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.async.AsyncListenerHandler;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import dev.imprex.orebfuscator.PermissionRequirements;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.obfuscation.ObfuscationPipeline;
import dev.imprex.orebfuscator.statistics.InjectorStatistics;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorCompatibility;
import net.imprex.orebfuscator.iterop.BukkitChunkPacketAccessor;
import net.imprex.orebfuscator.iterop.BukkitPlayerAccessor;
import net.imprex.orebfuscator.iterop.BukkitPlayerAccessorManager;
import net.imprex.orebfuscator.iterop.BukkitWorldAccessor;
import net.imprex.orebfuscator.util.ServerVersion;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ObfuscationAsyncListener extends PacketAdapter {

  private static final List<PacketType> PACKET_TYPES = Stream.concat(
      ObfuscationSyncListener.PACKET_TYPES_RESPAWN.stream(),
      Stream.of(
          PacketType.Play.Server.MAP_CHUNK,
          PacketType.Play.Server.CHUNK_BATCH_START,
          PacketType.Play.Server.CHUNK_BATCH_FINISHED,
          PacketType.Play.Server.UNLOAD_CHUNK,
          PacketType.Play.Server.CHUNKS_BIOMES,
          PacketType.Play.Server.LIGHT_UPDATE,
          PacketType.Play.Server.TILE_ENTITY_DATA,
          // Proximity hider updates
          PacketType.Play.Server.BLOCK_CHANGE,
          PacketType.Play.Server.MULTI_BLOCK_CHANGE,
          // Serverbound packet
          PacketType.Play.Client.CHUNK_BATCH_RECEIVED
      )).filter(PacketType::isSupported).toList();

  private final ObfuscationPipeline pipeline;
  private final InjectorStatistics statistics;
  private final BukkitPlayerAccessorManager playerManager;

  private final AsynchronousManager asynchronousManager;
  private final AsyncListenerHandler asyncListenerHandler;

  public ObfuscationAsyncListener(Orebfuscator orebfuscator) {
    super(orebfuscator, PACKET_TYPES);

    this.pipeline = orebfuscator.obfuscationPipeline();
    this.statistics = orebfuscator.statistics().injector;
    this.playerManager = orebfuscator.playerManager();

    this.asynchronousManager = ProtocolLibrary.getProtocolManager().getAsynchronousManager();
    this.asyncListenerHandler = this.asynchronousManager.registerAsyncHandler(this);

    if (ServerVersion.isFolia()) {
      OrebfuscatorCompatibility.runAsyncNow(this.asyncListenerHandler.getListenerLoop());
    } else {
      this.asyncListenerHandler.start();
    }
  }

  public void unregister() {
    this.asynchronousManager.unregisterAsyncHandler(this.asyncListenerHandler);
  }

  @Override
  public void onPacketReceiving(PacketEvent event) {
    statistics.injectorBatchSize.add(event.getPacket().getFloat().read(0));
  }

  @Override
  public void onPacketSending(PacketEvent event) {
    PacketType type = event.getPacketType();
    if (type != PacketType.Play.Server.MAP_CHUNK &&
        type != PacketType.Play.Server.CHUNK_BATCH_START &&
        type != PacketType.Play.Server.CHUNK_BATCH_FINISHED) {
      return;
    }

    BukkitPlayerAccessor player = this.playerManager.tryGet(event.getPlayer());
    if (player == null || !player.isAlive()) {
      return;
    }

    BukkitWorldAccessor world = player.world();
    if (player.hasPermission(PermissionRequirements.BYPASS) || !world.config().needsObfuscation()) {
      return;
    }

    if (type == PacketType.Play.Server.CHUNK_BATCH_START) {
      player.startBatch(asynchronousManager, event);
    } else if (type == PacketType.Play.Server.CHUNK_BATCH_FINISHED) {
      player.finishBatch();
    } else {
      var future = player.obfuscationFuture(event);
      if (future == null) {
        OfcLogger.warn("Processing chunk packet async without an obfuscation future, that shouldn't happen!");

        var packet = new BukkitChunkPacketAccessor(event.getPacket(), world);
        if (packet.isEmpty()) {
          future = CompletableFuture.completedFuture(null);
        } else {
          future = pipeline.request(world, player, packet, null).toCompletableFuture();
        }
      }

      if (!player.addBatchChunk(future)) {
        // no pending batch so we send each packet individually
        event.getAsyncMarker().incrementProcessingDelay();

        var timer = statistics.packetDelayChunk.start();
        future.whenComplete((result, throwable) -> {
          if (throwable != null) {
            OfcLogger.error(throwable);
          }

          this.asynchronousManager.signalPacketTransmission(event);
          timer.stop();
        });
      }
    }
  }
}
