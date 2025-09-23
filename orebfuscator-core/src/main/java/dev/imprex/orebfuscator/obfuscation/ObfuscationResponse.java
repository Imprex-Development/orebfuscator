package dev.imprex.orebfuscator.obfuscation;

import java.util.List;
import java.util.Set;
import dev.imprex.orebfuscator.util.BlockPos;

public record ObfuscationResponse(byte[] data, Set<BlockPos> blockEntities, List<BlockPos> proximityBlocks) {

}
