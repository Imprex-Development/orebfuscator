package net.imprex.orebfuscator.obfuscation;

import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.async.AsyncListenerHandler;
import com.comphenix.protocol.events.PacketEvent;

import net.imprex.orebfuscator.Orebfuscator;

public class ObfuscationListenerAsync extends ObfuscationListener {

	private final AsynchronousManager asynchronousManager;
	private final AsyncListenerHandler asyncListenerHandler;

	public ObfuscationListenerAsync(Orebfuscator orebfuscator) {
		super(orebfuscator);

		this.asynchronousManager = ProtocolLibrary.getProtocolManager().getAsynchronousManager();
		this.asyncListenerHandler = this.asynchronousManager.registerAsyncHandler(this);
		this.asyncListenerHandler.start(orebfuscator.getOrebfuscatorConfig().advanced().protocolLibThreads());
	}

	@Override
	protected void skipChunkForProcessing(PacketEvent event) {
		this.asynchronousManager.signalPacketTransmission(event);
	}

	@Override
	protected void preChunkProcessing(PacketEvent event) {
		event.getAsyncMarker().incrementProcessingDelay();
	}

	@Override
	protected void postChunkProcessing(PacketEvent event) {
		this.asynchronousManager.signalPacketTransmission(event);
	}

	@Override
	public void unregister() {
		this.asynchronousManager.unregisterAsyncHandler(this.asyncListenerHandler);
	}
}
