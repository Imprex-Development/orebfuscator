package net.imprex.orebfuscator.obfuscation;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.cache.ChunkCache;
import net.imprex.orebfuscator.cache.ChunkCacheRequest;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ObfuscatorSystem {

	private final Orebfuscator orebfuscator;
	private final OrebfuscatorConfig config;
	private final ChunkCache chunkCache;

	private final Obfuscator obfuscator;
	private final Deobfuscator deobfuscator;
	private AbstractChunkListener chunkListener;

	public ObfuscatorSystem(Orebfuscator orebfuscator) {
		this.orebfuscator = orebfuscator;
		this.config = orebfuscator.getOrebfuscatorConfig();
		this.chunkCache = orebfuscator.getChunkCache();

		this.obfuscator = new Obfuscator(orebfuscator);
		this.deobfuscator = new Deobfuscator(orebfuscator);
		Bukkit.getPluginManager().registerEvents(new DeobfuscationListener(orebfuscator, this.deobfuscator), orebfuscator);
	}

	public void registerChunkListener() {
		if (this.config.cache().enabled()) {
			this.chunkListener = new AsyncChunkListener(orebfuscator);
		} else {
			this.chunkListener = new SyncChunkListener(orebfuscator);
		}
	}

	public CompletableFuture<ObfuscatedChunk> obfuscateOrUseCache(ChunkStruct chunkStruct) {
		final ChunkPosition position = new ChunkPosition(chunkStruct.world, chunkStruct.chunkX, chunkStruct.chunkZ);
		final byte[] hash = ChunkCache.hash(this.config.hash(), chunkStruct.data);
		final ChunkCacheRequest request = new ChunkCacheRequest(this.obfuscator, position, hash, chunkStruct);

		if (this.config.cache().enabled()) {
			return this.chunkCache.get(request);
		} else {
			return this.obfuscator.obfuscate(request);
		}
	}

	public void deobfuscate(Collection<? extends Block> blocks) {
		this.deobfuscator.deobfuscate(blocks);
	}

	public void close() {
		if (this.chunkListener != null) {
			this.chunkListener.unregister();
		}
	}
}
