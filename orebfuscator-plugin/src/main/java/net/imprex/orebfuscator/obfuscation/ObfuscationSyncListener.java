package net.imprex.orebfuscator.obfuscation;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import dev.imprex.orebfuscator.PermissionRequirements;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.obfuscation.ObfuscationPipeline;
import dev.imprex.orebfuscator.statistics.InjectorStatistics;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.iterop.BukkitChunkPacketAccessor;
import net.imprex.orebfuscator.iterop.BukkitPlayerAccessor;
import net.imprex.orebfuscator.iterop.BukkitPlayerAccessorManager;
import net.imprex.orebfuscator.iterop.BukkitWorldAccessor;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ObfuscationSyncListener extends PacketAdapter {

  public static final List<PacketType> PACKET_TYPES_RESPAWN = Stream.of(
      // 1.16.5
      PacketType.Play.Server.RESPAWN,
      PacketType.Play.Server.VIEW_DISTANCE,
      PacketType.Play.Server.POSITION,
      PacketType.Play.Server.SPAWN_POSITION,
      PacketType.Play.Server.SERVER_DIFFICULTY,
      PacketType.Play.Server.EXPERIENCE,
      PacketType.Play.Server.WORLD_BORDER,
      PacketType.Play.Server.UPDATE_TIME,
      PacketType.Play.Server.GAME_STATE_CHANGE,
      PacketType.Play.Server.ENTITY_STATUS,
      PacketType.Play.Server.COMMANDS,
      PacketType.Play.Server.NAMED_SOUND_EFFECT,
      PacketType.Play.Server.HELD_ITEM_SLOT,
      PacketType.Play.Server.WINDOW_ITEMS,
      PacketType.Play.Server.WINDOW_DATA,
      PacketType.Play.Server.SET_SLOT,
      PacketType.Play.Server.UPDATE_ATTRIBUTES,
      PacketType.Play.Server.UPDATE_HEALTH,
      PacketType.Play.Server.ABILITIES,
      PacketType.Play.Server.ENTITY_EFFECT,
      // 1.21.11
      PacketType.Play.Server.UPDATE_SIMULATION_DISTANCE,
      PacketType.Play.Server.INITIALIZE_BORDER
  ).filter(PacketType::isSupported).toList();

  private static final List<PacketType> PACKET_TYPES = Stream.concat(
      Stream.of(PacketType.Play.Server.MAP_CHUNK),
      PACKET_TYPES_RESPAWN.stream()
  ).toList();

  private final ObfuscationPipeline pipeline;
  private final InjectorStatistics statistics;
  private final BukkitPlayerAccessorManager playerManager;

  private final ProtocolManager protocolManager;

  public ObfuscationSyncListener(Orebfuscator orebfuscator) {
    super(params()
        .plugin(orebfuscator)
        .types(PACKET_TYPES.toArray(PacketType[]::new))
        .optionAsync());

    this.pipeline = orebfuscator.obfuscationPipeline();
    this.statistics = orebfuscator.statistics().injector;
    this.playerManager = orebfuscator.playerManager();

    this.protocolManager = ProtocolLibrary.getProtocolManager();
    this.protocolManager.addPacketListener(this);
  }

  public void unregister() {
    this.protocolManager.removePacketListener(this);
  }

  @Override
  public void onPacketSending(PacketEvent event) {
    var timer = statistics.injectorDelaySync.start();
    try {
      BukkitPlayerAccessor player = this.playerManager.tryGet(event.getPlayer());
      if (player == null) {
        if (event.getAsyncMarker() != null) {
          // cancel async processing of login packets as our player object is created after join event
          event.getAsyncMarker().setAsyncCancelled(true);
        }
        return;
      }

      PacketType type = event.getPacketType();
      if (type == PacketType.Play.Server.RESPAWN) {
        player.startRespawn();
      }

      if (type == PacketType.Play.Server.MAP_CHUNK) {
        this.onSendLevelChunk(event, player);
      } else if (event.getAsyncMarker() != null && !player.isRespawning()) {
        // don't async delay packets if the player isn't respawning
        event.getAsyncMarker().setAsyncCancelled(true);
      }
    } finally {
      timer.stop();
    }
  }

  private void onSendLevelChunk(PacketEvent event, BukkitPlayerAccessor player) {
    BukkitWorldAccessor world = player.world();
    if (player.hasPermission(PermissionRequirements.BYPASS) || !world.config().needsObfuscation()) {
      return;
    }

    var packet = new BukkitChunkPacketAccessor(event.getPacket(), world);
    if (packet.isEmpty()) {
      return;
    }

    var neighboringChunks = world.getNeighboringChunksNow(packet.chunkX(), packet.chunkZ());
    var future = pipeline.request(world, player, packet, neighboringChunks).toCompletableFuture();

    player.obfuscationFuture(event, future);
  }
}
