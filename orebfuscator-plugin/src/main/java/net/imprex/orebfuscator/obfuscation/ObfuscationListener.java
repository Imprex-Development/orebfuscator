package net.imprex.orebfuscator.obfuscation;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import net.imprex.orebfuscator.Orebfuscator;

public class ObfuscationListener extends PacketAdapter {

	private final ProtocolManager protocolManager;

	public ObfuscationListener(Orebfuscator orebfuscator) {
		super(PacketAdapter.params()
				.plugin(orebfuscator)
				.types(PacketType.Play.Client.CHUNK_BATCH_RECEIVED)
				.optionAsync());

		this.protocolManager = ProtocolLibrary.getProtocolManager();
		this.protocolManager.addPacketListener(this);
	}

	public void unregister() {
		this.protocolManager.removePacketListener(this);
	}

	@Override
	public void onPacketReceiving(PacketEvent event) {
		event.getPacket().getFloat().write(0, 10f);
	}
}
