package net.imprex.orebfuscator.proximityhider;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.ProximityConfig;
import net.imprex.orebfuscator.util.PermissionUtil;

public class ProximityPacketListener extends PacketAdapter {

	private final ProtocolManager protocolManager;

	private final OrebfuscatorConfig config;

	private final ProximityPlayerManager playerManager;

	public ProximityPacketListener(Orebfuscator orebfuscator) {
		super(orebfuscator, PacketType.Play.Server.UNLOAD_CHUNK);

		this.protocolManager = ProtocolLibrary.getProtocolManager();
		this.protocolManager.addPacketListener(this);

		this.config = orebfuscator.getOrebfuscatorConfig();
		this.playerManager = orebfuscator.getProximityHider().getPlayerManager();
	}

	public void unregister() {
		this.protocolManager.removePacketListener(this);
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		Player player = event.getPlayer();
		if (PermissionUtil.canDeobfuscate(player)) {
			return;
		}

		World world = player.getWorld();
		ProximityConfig proximityConfig = config.world(world).proximity();
		if (proximityConfig == null || !proximityConfig.isEnabled()) {
			return;
		}

		StructureModifier<Integer> ints = event.getPacket().getIntegers();
		this.playerManager.removeChunk(player, ints.read(0), ints.read(1));
	}
}
