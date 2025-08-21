package net.imprex.orebfuscator.compatibility.bukkit;

import java.util.concurrent.CompletableFuture;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import net.imprex.orebfuscator.compatibility.CompatibilityLayer;
import net.imprex.orebfuscator.compatibility.CompatibilityScheduler;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.util.ChunkPosition;

public class BukkitCompatibilityLayer implements CompatibilityLayer {

	private final Thread mainThread = Thread.currentThread();

	private final BukkitScheduler scheduler;
	private final BukkitChunkLoader chunkLoader;

	public BukkitCompatibilityLayer(Plugin plugin, Config config) {
		this.scheduler = new BukkitScheduler(plugin);
		this.chunkLoader = new BukkitChunkLoader(plugin, config);
	}

	@Override
	public boolean isGameThread() {
		return Thread.currentThread() == this.mainThread;
	}

	@Override
	public CompatibilityScheduler getScheduler() {
		return this.scheduler;
	}

	@Override
	public CompletableFuture<ReadOnlyChunk[]> getNeighboringChunks(World world, ChunkPosition position) {
		return this.chunkLoader.submitRequest(world, position);
	}
}
