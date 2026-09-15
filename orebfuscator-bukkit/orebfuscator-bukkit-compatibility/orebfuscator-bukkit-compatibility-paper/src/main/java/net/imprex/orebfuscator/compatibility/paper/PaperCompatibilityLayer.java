package net.imprex.orebfuscator.compatibility.paper;

import dev.imprex.orebfuscator.config.api.Config;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.compatibility.spigot.SpigotCompatibilityLayer;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.CompletableFuture;

@NullMarked
public class PaperCompatibilityLayer extends SpigotCompatibilityLayer {

  public PaperCompatibilityLayer(Plugin plugin, Config config) {
    super(plugin, config);
  }

  @Override
  public CompletableFuture<ChunkAccessor> getChunkFuture(World world, int chunkX, int chunkZ) {
    return world.getChunkAtAsync(chunkX, chunkZ).thenApply(unused -> {
      var chunk = OrebfuscatorNms.getChunkNow(world, chunkX, chunkZ);
      return ChunkAccessor.ofNullable(chunk);
    });
  }
}
