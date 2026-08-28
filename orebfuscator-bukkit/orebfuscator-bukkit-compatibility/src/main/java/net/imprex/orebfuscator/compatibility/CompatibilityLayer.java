package net.imprex.orebfuscator.compatibility;

import dev.imprex.orebfuscator.interop.ChunkAccessor;
import org.bukkit.World;
import org.jspecify.annotations.NullMarked;

import java.util.concurrent.CompletableFuture;

@NullMarked
public interface CompatibilityLayer {

  boolean isGameThread();

  CompatibilityScheduler getScheduler();

  CompletableFuture<ChunkAccessor> getChunkFuture(World world, int chunkX, int chunkZ);
}
