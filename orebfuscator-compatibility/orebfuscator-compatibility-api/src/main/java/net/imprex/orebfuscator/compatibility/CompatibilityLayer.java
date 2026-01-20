package net.imprex.orebfuscator.compatibility;

import java.util.concurrent.CompletableFuture;

import org.bukkit.World;

import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.obfuscation.ObfuscationRequest;

public interface CompatibilityLayer {

  boolean isGameThread();

  CompatibilityScheduler getScheduler();

  CompletableFuture<ChunkAccessor[]> getNeighboringChunks(World world, ObfuscationRequest request);
}
