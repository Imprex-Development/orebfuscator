package net.imprex.orebfuscator.proximity;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.ChunkCoordIntPair;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.chunk.ChunkCapabilities;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.ProximityConfig;
import net.imprex.orebfuscator.player.OrebfuscatorPlayer;
import net.imprex.orebfuscator.player.OrebfuscatorPlayerMap;
import net.imprex.orebfuscator.util.PermissionUtil;

public class ProximityPacketListener extends PacketAdapter {

  private final ProtocolManager protocolManager;

  private final OrebfuscatorConfig config;
  private final OrebfuscatorPlayerMap playerMap;

  public ProximityPacketListener(Orebfuscator orebfuscator) {
    super(orebfuscator, PacketType.Play.Server.UNLOAD_CHUNK);

    this.protocolManager = ProtocolLibrary.getProtocolManager();
    this.protocolManager.addPacketListener(this);

    this.config = orebfuscator.getOrebfuscatorConfig();
    this.playerMap = orebfuscator.getPlayerMap();
  }

  public void unregister() {
    this.protocolManager.removePacketListener(this);
  }

  @Override
  public void onPacketSending(PacketEvent event) {
    Player player = event.getPlayer();
    if (PermissionUtil.canBypassObfuscate(player)) {
      return;
    }

    World world = player.getWorld();
    ProximityConfig proximityConfig = config.world(world).proximity();
    if (proximityConfig == null || !proximityConfig.isEnabled()) {
      return;
    }

    OrebfuscatorPlayer orebfuscatorPlayer = this.playerMap.get(player);
    if (orebfuscatorPlayer != null) {
      PacketContainer packet = event.getPacket();
      if (ChunkCapabilities.hasChunkPosFieldUnloadPacket()) {
        ChunkCoordIntPair chunkPos = packet.getChunkCoordIntPairs().read(0);
        orebfuscatorPlayer.removeChunk(chunkPos.getChunkX(), chunkPos.getChunkZ());
      } else {
        StructureModifier<Integer> ints = packet.getIntegers();
        orebfuscatorPlayer.removeChunk(ints.read(0), ints.read(1));
      }
    }
  }
}
