package net.imprex.orebfuscator.obfuscation;

import net.imprex.orebfuscator.iterop.BukkitPlayerAccessorManager;
import org.jspecify.annotations.NullMarked;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import dev.imprex.orebfuscator.PermissionRequirements;
import dev.imprex.orebfuscator.obfuscation.ObfuscationPipeline;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.iterop.BukkitChunkPacketAccessor;
import net.imprex.orebfuscator.iterop.BukkitPlayerAccessor;
import net.imprex.orebfuscator.iterop.BukkitWorldAccessor;

@NullMarked
public class ObfuscationSyncListener extends PacketAdapter {

  private final ObfuscationPipeline pipeline;
  private final BukkitPlayerAccessorManager playerManager;

  private final ProtocolManager protocolManager;

  public ObfuscationSyncListener(Orebfuscator orebfuscator) {
    super(orebfuscator, PacketType.Play.Server.MAP_CHUNK);

    this.pipeline = orebfuscator.obfuscationPipeline();
    this.playerManager = orebfuscator.playerManager();

    this.protocolManager = ProtocolLibrary.getProtocolManager();
    this.protocolManager.addPacketListener(this);
  }

  public void unregister() {
    this.protocolManager.removePacketListener(this);
  }

  @Override
  public void onPacketSending(PacketEvent event) {
    BukkitPlayerAccessor player = this.playerManager.tryGet(event.getPlayer());
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

    var neighboringChunks = world.getNeighboringChunks(packet.chunkX(), packet.chunkZ());
    var future = pipeline.request(world, player, packet, neighboringChunks).toCompletableFuture();

    player.obfuscationFuture(event, future);
  }
}
