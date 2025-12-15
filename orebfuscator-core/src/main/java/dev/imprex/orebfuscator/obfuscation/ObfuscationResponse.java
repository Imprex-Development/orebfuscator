package dev.imprex.orebfuscator.obfuscation;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.player.ProximityBlock;
import dev.imprex.orebfuscator.util.BlockPos;

@NullMarked
public record ObfuscationResponse(byte[] data, Set<BlockPos> blockEntities, List<ProximityBlock> proximityBlocks) {

  public ObfuscationResponse {
    Objects.requireNonNull(data);
    Objects.requireNonNull(blockEntities);
    Objects.requireNonNull(proximityBlocks);
  }
}
