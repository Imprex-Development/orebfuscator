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
import dev.imprex.orebfuscator.interop.ChunkPacketAccessor;
import dev.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import dev.imprex.orebfuscator.util.ChunkDirection;
import net.imprex.orebfuscator.OrebfuscatorNms;

public class BukkitChunkLoader implements Runnable {

  private final Queue<Task> tasks = new ConcurrentLinkedQueue<>();

  private final long availableNanosPerTick;

  public BukkitChunkLoader(Plugin plugin, Config config) {
    this.availableNanosPerTick = TimeUnit.MILLISECONDS.toNanos(config.advanced().maxMillisecondsPerTick());

    Bukkit.getScheduler().runTaskTimer(plugin, this, 0, 1);
  }

  public CompletableFuture<ChunkAccessor[]> submitRequest(World world, ObfuscationRequest request) {
    Task task = new Task(world, request);
    this.tasks.offer(task);
    return task.future;
  }

  @Override
  public void run() {
    final long time = System.nanoTime();

    Task task = null;
    while (System.nanoTime() - time < this.availableNanosPerTick && (task = this.tasks.poll()) != null) {
      task.run();
    }
  }

  private class Task implements Runnable {

    private final World world;
    private final ObfuscationRequest request;

    private final CompletableFuture<ChunkAccessor[]> future = new CompletableFuture<>();

    public Task(World world, ObfuscationRequest request) {
      this.world = world;
      this.request = request;
    }

    @Override
    public void run() {
      final ChunkPacketAccessor packet = this.request.packet();
      final ChunkAccessor[] neighboringChunks = new ChunkAccessor[4];

      for (ChunkDirection direction : ChunkDirection.values()) {
        int chunkX = packet.chunkX() + direction.getOffsetX();
        int chunkZ = packet.chunkZ() + direction.getOffsetZ();

        int index = direction.ordinal();
        var chunk = this.request.neighborChunks()[index];

        if (chunk != null) {
          neighboringChunks[index] = chunk;
        } else {
          neighboringChunks[index] = OrebfuscatorNms.getChunkAccessor(world, chunkX, chunkZ);
        }
      }

      future.complete(neighboringChunks);
    }
  }
}
