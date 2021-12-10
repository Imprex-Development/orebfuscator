package net.imprex.orebfuscator.obfuscation;

import java.util.Collection;
import java.util.concurrent.CompletionStage;

import org.bukkit.block.Block;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.cache.ObfuscationCache;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;

public class ObfuscationSystem {

	private final Orebfuscator orebfuscator;
	private final OrebfuscatorConfig config;
	private final ObfuscationCache cache;

	private final ObfuscationProcessor processor;
	private final ObfuscationTaskDispatcher dispatcher;
	private ObfuscationListener listener;

	private final DeobfuscationWorker deobfuscationWorker;

	public ObfuscationSystem(Orebfuscator orebfuscator) {
		this.orebfuscator = orebfuscator;
		this.config = orebfuscator.getOrebfuscatorConfig();
		this.cache = orebfuscator.getObfuscationCache();

		this.processor = new ObfuscationProcessor(orebfuscator);
		this.dispatcher = new ObfuscationTaskDispatcher(orebfuscator, this.processor);

		this.deobfuscationWorker = new DeobfuscationWorker(orebfuscator);
		DeobfuscationListener.createAndRegister(orebfuscator, this.deobfuscationWorker);
	}

	public void registerChunkListener() {
		this.listener = new ObfuscationListener(orebfuscator);
	}

	public CompletionStage<ObfuscationResult> obfuscate(ChunkStruct chunkStruct) {
		ObfuscationRequest request = ObfuscationRequest.fromChunk(chunkStruct, this.config, this.dispatcher);
		if (this.config.cache().enabled()) {
			return this.cache.get(request);
		} else {
			return request.submitForObfuscation();
		}
	}

	public void deobfuscate(Collection<? extends Block> blocks) {
		this.deobfuscationWorker.deobfuscate(blocks, false);
	}

	public void shutdown() {
		this.listener.unregister();
		this.dispatcher.shutdown();
	}
}
