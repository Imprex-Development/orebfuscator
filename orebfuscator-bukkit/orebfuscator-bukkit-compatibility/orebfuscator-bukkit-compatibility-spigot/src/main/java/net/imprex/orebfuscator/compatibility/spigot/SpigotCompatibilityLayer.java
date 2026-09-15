package net.imprex.orebfuscator.compatibility.spigot;

import dev.imprex.orebfuscator.config.api.Config;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.compatibility.CompatibilityLayer;
import net.imprex.orebfuscator.compatibility.CompatibilityScheduler;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.CompletableFuture;

@NullMarked
public class SpigotCompatibilityLayer implements CompatibilityLayer {

  private final Thread mainThread = Thread.currentThread();

  private final SpigotScheduler scheduler;

  public SpigotCompatibilityLayer(Plugin plugin, Config config) {
    this.scheduler = new SpigotScheduler(plugin);
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
  public CompletableFuture<ChunkAccessor> getChunkFuture(World world, int chunkX, int chunkZ) {
    return OrebfuscatorNms.getChunkFuture(world, chunkX, chunkZ)
        .thenApply(ChunkAccessor::ofNullable);
  }
}
