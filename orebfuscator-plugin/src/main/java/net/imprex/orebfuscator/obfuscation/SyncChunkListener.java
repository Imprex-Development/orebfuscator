package net.imprex.orebfuscator.obfuscation;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketEvent;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.proximityhider.ProximityPlayerManager;

public class SyncChunkListener extends AbstractChunkListener {

	private final ProtocolManager protocolManager;

	private final ProximityPlayerManager proximityManager;

	public SyncChunkListener(Orebfuscator orebfuscator) {
		super(orebfuscator);

		this.protocolManager = ProtocolLibrary.getProtocolManager();
		this.protocolManager.addPacketListener(this);

		this.proximityManager = orebfuscator.getProximityHider().getPlayerManager();
	}

	@Override
	public void unregister() {
		this.protocolManager.removePacketListener(this);
	}

	@Override
	protected void postChunkProcessing(PacketEvent event, ChunkStruct struct, ObfuscatedChunk chunk) {
		this.proximityManager.addChunk(event.getPlayer(), struct.chunkX, struct.chunkZ, chunk.getProximityBlocks());
	}
}
