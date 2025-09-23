package net.imprex.orebfuscator.compatibility.bukkit;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import dev.imprex.orebfuscator.config.api.Config;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.util.ChunkDirection;
import net.imprex.orebfuscator.OrebfuscatorNms;

public class BukkitChunkLoader implements Runnable {

	private final Queue<Request> requests = new ConcurrentLinkedQueue<>();

	private final long availableNanosPerTick;

	public BukkitChunkLoader(Plugin plugin, Config config) {
		this.availableNanosPerTick = TimeUnit.MILLISECONDS.toNanos(config.advanced().maxMillisecondsPerTick());

		Bukkit.getScheduler().runTaskTimer(plugin, this, 0, 1);
	}

	public CompletableFuture<ChunkAccessor[]> submitRequest(World world, int chunkX, int chunkZ) {
		Request request = new Request(world, chunkX, chunkZ);
		this.requests.offer(request);
		return request.future;
	}

	@Override
	public void run() {
		final long time = System.nanoTime();

		Request request = null;
		while (System.nanoTime() - time < this.availableNanosPerTick && (request = this.requests.poll()) != null) {
			request.run();
		}
	}

	private class Request implements Runnable {

		private final World world;
		private final int chunkX;
		private final int chunkZ;

		private final CompletableFuture<ChunkAccessor[]> future = new CompletableFuture<>();

		public Request(World world, int chunkX, int chunkZ) {
			this.world = world;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
		}

		@Override
		public void run() {
			final ChunkAccessor[] neighboringChunks = new ChunkAccessor[4];

			for (ChunkDirection direction : ChunkDirection.values()) {
				int chunkX = this.chunkX + direction.getOffsetX();
				int chunkZ = this.chunkZ + direction.getOffsetZ();

				neighboringChunks[direction.ordinal()] = OrebfuscatorNms.getReadOnlyChunk(world, chunkX, chunkZ);
			}

			future.complete(neighboringChunks);
		}
	}
}
