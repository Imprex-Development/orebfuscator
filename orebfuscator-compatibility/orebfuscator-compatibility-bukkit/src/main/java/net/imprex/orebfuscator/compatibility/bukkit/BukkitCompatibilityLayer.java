package net.imprex.orebfuscator.compatibility.bukkit;

import java.util.concurrent.CompletableFuture;

import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import dev.imprex.orebfuscator.config.api.Config;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import net.imprex.orebfuscator.compatibility.CompatibilityLayer;
import net.imprex.orebfuscator.compatibility.CompatibilityScheduler;

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
	public CompletableFuture<ChunkAccessor[]> getNeighboringChunks(World world, int chunkX, int chunkZ) {
		return this.chunkLoader.submitRequest(world, chunkX, chunkZ);
	}
}
