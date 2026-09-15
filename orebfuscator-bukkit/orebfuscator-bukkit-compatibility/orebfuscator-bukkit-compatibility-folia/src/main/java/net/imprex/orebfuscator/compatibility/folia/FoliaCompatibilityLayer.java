package net.imprex.orebfuscator.compatibility.folia;

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
public class FoliaCompatibilityLayer implements CompatibilityLayer {

  private static final Class<?> TICK_THREAD_CLASS = getTickThreadClass();

  private static Class<?> getTickThreadClass() {
    try {
      return Class.forName("io.papermc.paper.threadedregions.TickRegionScheduler$TickThreadRunner");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Can't find tick thread class for folia", e);
    }
  }

  private final FoliaScheduler scheduler;

  public FoliaCompatibilityLayer(Plugin plugin, Config config) {
    this.scheduler = new FoliaScheduler(plugin);
  }

  @Override
  public boolean isGameThread() {
    return TICK_THREAD_CLASS.isInstance(Thread.currentThread());
  }

  @Override
  public CompatibilityScheduler getScheduler() {
    return this.scheduler;
  }

  @Override
  public CompletableFuture<ChunkAccessor> getChunkFuture(World world, int chunkX, int chunkZ) {
    return world.getChunkAtAsync(chunkX, chunkZ).thenApply(unused -> {
      var chunk = OrebfuscatorNms.getChunkNow(world, chunkX, chunkZ);
      return ChunkAccessor.ofNullable(chunk);
    });
  }
}
