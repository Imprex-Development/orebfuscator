package net.imprex.orebfuscator.compatibility.paper;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.bukkit.World;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.interop.ChunkPacketAccessor;
import dev.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import dev.imprex.orebfuscator.util.ChunkDirection;
import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.compatibility.CompatibilityLayer;

public abstract class AbstractPaperCompatibilityLayer implements CompatibilityLayer {

  @Override
  public CompletableFuture<ChunkAccessor[]> getNeighboringChunks(World world, ObfuscationRequest request) {
    if (!Arrays.stream(request.neighborChunks()).anyMatch(Objects::isNull)) {
      return CompletableFuture.completedFuture(request.neighborChunks());
    }

    final ChunkPacketAccessor packet = request.packet();
    final CompletableFuture<?>[] futures = new CompletableFuture<?>[4];
    final ChunkAccessor[] neighboringChunks = new ChunkAccessor[4];

    for (ChunkDirection direction : ChunkDirection.values()) {
      int chunkX = packet.chunkX() + direction.getOffsetX();
      int chunkZ = packet.chunkZ() + direction.getOffsetZ();

      int index = direction.ordinal();
      var chunk = request.neighborChunks()[index];

      if (chunk != null) {
        futures[index] = CompletableFuture.completedFuture(chunk);
      } else {
        futures[index] = world.getChunkAtAsync(chunkX, chunkZ).thenAccept(c -> {
          neighboringChunks[index] = OrebfuscatorNms.getChunkAccessor(world, chunkX, chunkZ);
        });
      }
    }

    return CompletableFuture.allOf(futures).thenApply(v -> neighboringChunks);
  }
}
