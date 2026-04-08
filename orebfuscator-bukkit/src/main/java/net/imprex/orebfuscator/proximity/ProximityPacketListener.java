package net.imprex.orebfuscator.proximity;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;
import dev.imprex.orebfuscator.PermissionRequirements;
import dev.imprex.orebfuscator.config.api.ProximityConfig;
import dev.imprex.orebfuscator.player.OrebfuscatorPlayer;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.iterop.BukkitPlayerAccessor;
import net.imprex.orebfuscator.iterop.BukkitPlayerAccessorManager;
import net.imprex.orebfuscator.iterop.BukkitWorldAccessor;
import net.imprex.orebfuscator.util.MinecraftVersion;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ProximityPacketListener extends PacketAdapter {

  private static final boolean HAS_CHUNK_POS_FIELD = MinecraftVersion.isAtOrAbove("1.20.2");

  private final BukkitPlayerAccessorManager playerManager;
  private final ProtocolManager protocolManager;

  public ProximityPacketListener(Orebfuscator orebfuscator) {
    super(orebfuscator, PacketType.Play.Server.UNLOAD_CHUNK);

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
    if (player == null || player.hasPermission(PermissionRequirements.BYPASS)) {
      return;
    }

    BukkitWorldAccessor world = player.world();
    ProximityConfig proximityConfig = world.config().proximity();
    if (proximityConfig == null || !proximityConfig.isEnabled()) {
      return;
    }

    PacketContainer packet = event.getPacket();
    OrebfuscatorPlayer orebfuscatorPlayer = player.orebfuscatorPlayer();

    if (HAS_CHUNK_POS_FIELD) {
      ChunkCoordIntPair chunkPos = packet.getChunkCoordIntPairs().read(0);
      orebfuscatorPlayer.removeChunk(world, chunkPos.getChunkX(), chunkPos.getChunkZ());
    } else {
      StructureModifier<Integer> ints = packet.getIntegers();
      orebfuscatorPlayer.removeChunk(world, ints.read(0), ints.read(1));
    }
  }
}
