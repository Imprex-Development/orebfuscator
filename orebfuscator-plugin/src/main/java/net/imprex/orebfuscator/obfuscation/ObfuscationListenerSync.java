package net.imprex.orebfuscator.obfuscation;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketEvent;

import net.imprex.orebfuscator.Orebfuscator;

public class ObfuscationListenerSync extends ObfuscationListener {

	private final ProtocolManager protocolManager;

	public ObfuscationListenerSync(Orebfuscator orebfuscator) {
		super(orebfuscator);

		this.protocolManager = ProtocolLibrary.getProtocolManager();
		this.protocolManager.addPacketListener(this);
	}

	@Override
	protected void preChunkProcessing(PacketEvent event) {
		event.setCancelled(true);
	}

	@Override
	protected void postChunkProcessing(PacketEvent event) {
		this.protocolManager.sendServerPacket(event.getPlayer(), event.getPacket(), false);
	}

	@Override
	public void unregister() {
		this.protocolManager.removePacketListener(this);
	}
}
