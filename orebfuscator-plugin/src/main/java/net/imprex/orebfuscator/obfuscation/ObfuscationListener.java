package net.imprex.orebfuscator.obfuscation;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.async.AsyncListenerHandler;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import dev.imprex.orebfuscator.PermissionRequirements;
import dev.imprex.orebfuscator.obfuscation.ObfuscationPipeline;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorCompatibility;
import net.imprex.orebfuscator.iterop.BukkitChunkPacketAccessor;
import net.imprex.orebfuscator.iterop.BukkitPlayerAccessor;
import net.imprex.orebfuscator.iterop.BukkitWorldAccessor;
import net.imprex.orebfuscator.util.ServerVersion;

// TODO: Nullability
// TODO: add sync map chunk packet listener that starts obfuscation on the main thread and tries to get neighboring chunks
//       now to reduce the potential wait time for async chunk requests.
// TODO: add support for chunk batch system in sync listener (cancel) any packets and track current batch. On batch delimiter use
//       future.all and send all packets at once for server to trigger multple write and single flush.
// TODO: maybe replace async listener entirely with sync listener and packet queue
public class ObfuscationListener extends PacketAdapter {

  private static final List<PacketType> PACKET_TYPES = Arrays.asList(
      PacketType.Play.Server.MAP_CHUNK,
      PacketType.Play.Server.UNLOAD_CHUNK,
      PacketType.Play.Server.CHUNKS_BIOMES,
      PacketType.Play.Server.LIGHT_UPDATE,
      PacketType.Play.Server.TILE_ENTITY_DATA,
      PacketType.Play.Server.RESPAWN,
      // PlayerList::sendLevelInfo
      PacketType.Play.Server.INITIALIZE_BORDER,
      PacketType.Play.Server.UPDATE_TIME,
      PacketType.Play.Server.SPAWN_POSITION,
      PacketType.Play.Server.GAME_STATE_CHANGE,
      // Proximity hider updates
      PacketType.Play.Server.BLOCK_CHANGE,
      PacketType.Play.Server.MULTI_BLOCK_CHANGE,
      // Clientbound packet
      PacketType.Play.Client.CHUNK_BATCH_RECEIVED
  );

  private final ObfuscationPipeline pipeline;

  private final AsynchronousManager asynchronousManager;
  private final AsyncListenerHandler asyncListenerHandler;

  public ObfuscationListener(Orebfuscator orebfuscator) {
    super(orebfuscator, PACKET_TYPES.stream()
        .filter(Objects::nonNull)
        .filter(PacketType::isSupported)
        .collect(Collectors.toList()));

    this.pipeline = orebfuscator.obfuscationPipeline();

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
    event.getPacket().getFloat().write(0, 10f);
  }

  @Override
  public void onPacketSending(PacketEvent event) {
    if (event.getPacket().getType() != PacketType.Play.Server.MAP_CHUNK) {
      return;
    }

    BukkitPlayerAccessor player = BukkitPlayerAccessor.tryGet(event.getPlayer());
    if (player == null || !player.isAlive()) {
      return;
    }
    
    BukkitWorldAccessor world = player.world();
    if (player.hasPermission(PermissionRequirements.BYPASS) || !world.config().needsObfuscation()) {
      return;
    }

    var packet = new BukkitChunkPacketAccessor(event.getPacket(), world);
    if (packet.isEmpty()) {
      return;
    }

    var neighboringChunks = world
        .getNeighboringChunks(packet.chunkX(), packet.chunkZ());
    var future = pipeline.request(world, player, packet, neighboringChunks)
        .toCompletableFuture();
    
    if (!future.isDone()) {
      event.getAsyncMarker().incrementProcessingDelay();
      future.whenComplete((result, throwable) -> {
        this.asynchronousManager.signalPacketTransmission(event);
      });
    }
  }
}
