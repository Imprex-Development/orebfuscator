package net.imprex.orebfuscator.obfuscation;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.async.AsyncListenerHandler;
import com.comphenix.protocol.events.PacketEvent;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.proximityhider.ProximityPlayerManager;

public class AsyncChunkListener extends AbstractChunkListener {

	private final AsynchronousManager asynchronousManager;
	private final AsyncListenerHandler asyncListenerHandler;

	private final ProximityPlayerManager proximityManager;

	public AsyncChunkListener(Orebfuscator orebfuscator) {
		super(orebfuscator);

		this.asynchronousManager = ProtocolLibrary.getProtocolManager().getAsynchronousManager();
		this.asyncListenerHandler = this.asynchronousManager.registerAsyncHandler(this);
		this.asyncListenerHandler.start(orebfuscator.getOrebfuscatorConfig().cache().protocolLibThreads());

		this.proximityManager = orebfuscator.getProximityHider().getPlayerManager();
	}

	@Override
	public void unregister() {
		this.asynchronousManager.unregisterAsyncHandler(this.asyncListenerHandler);
	}

	@Override
	protected void skipChunkForProcessing(PacketEvent event) {
		this.asynchronousManager.signalPacketTransmission(event);
	}

	@Override
	protected void preChunkProcessing(PacketEvent event, ChunkStruct struct) {
		event.getAsyncMarker().incrementProcessingDelay();
	}

	@Override
	protected void postChunkProcessing(PacketEvent event, ChunkStruct struct, ObfuscatedChunk chunk) {
		Player player = event.getPlayer();
		this.proximityManager.addAndLockChunk(player, struct.chunkX, struct.chunkZ, chunk.getProximityBlocks());

		Bukkit.getScheduler().runTask(this.plugin, () -> {
			this.asynchronousManager.signalPacketTransmission(event);
			this.proximityManager.unlockChunk(player, struct.chunkX, struct.chunkZ);
		});
	}

}
