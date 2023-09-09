package net.imprex.orebfuscator.compatibility.bukkit;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.util.ChunkDirection;
import net.imprex.orebfuscator.util.ChunkPosition;

public class BukkitChunkLoader implements Runnable {

	private final Queue<Request> requests = new ConcurrentLinkedQueue<>();

	private final long availableNanosPerTick;

	public BukkitChunkLoader(Plugin plugin, Config config) {
		this.availableNanosPerTick = TimeUnit.MILLISECONDS.toNanos(config.advanced().maxMillisecondsPerTick());

		Bukkit.getScheduler().runTaskTimer(plugin, this, 0, 1);
	}

	public CompletableFuture<ReadOnlyChunk[]> submitRequest(World world, ChunkPosition chunkPosition) {
		Request request = new Request(world, chunkPosition);
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
		private final ChunkPosition position;

		private final CompletableFuture<ReadOnlyChunk[]> future = new CompletableFuture<>();

		public Request(World world, ChunkPosition position) {
			this.world = world;
			this.position = position;
		}

		@Override
		public void run() {
			final ReadOnlyChunk[] neighboringChunks = new ReadOnlyChunk[4];

			for (ChunkDirection direction : ChunkDirection.values()) {
				int chunkX = position.x + direction.getOffsetX();
				int chunkZ = position.z + direction.getOffsetZ();

				neighboringChunks[direction.ordinal()] = OrebfuscatorNms.getReadOnlyChunk(world, chunkX, chunkZ);
			}

			future.complete(neighboringChunks);
		}
	}
}
