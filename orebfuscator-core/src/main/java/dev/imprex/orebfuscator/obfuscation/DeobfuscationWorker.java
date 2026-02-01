package dev.imprex.orebfuscator.obfuscation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.cache.ObfuscationCache;
import dev.imprex.orebfuscator.config.OrebfuscatorConfig;
import dev.imprex.orebfuscator.config.api.BlockFlags;
import dev.imprex.orebfuscator.config.api.ObfuscationConfig;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.statistics.ObfuscationStatistics;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.ChunkCacheKey;

@NullMarked
public class DeobfuscationWorker {

  private static List<BlockPos> precomputeOffsets(int radius) {
    List<Entry<BlockPos, Integer>> offset = new ArrayList<>();

    for (int dx = -radius; dx <= radius; dx++) {
      for (int dy = -radius; dy <= radius; dy++) {
        for (int dz = -radius; dz <= radius; dz++) {
          int distance = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
          if (distance <= radius) {
            offset.add(Map.entry(new BlockPos(dx, dy, dz), distance));
          }
        }
      }
    }

    offset
        .sort(Comparator.comparingInt((Entry<BlockPos, Integer> e) -> e.getValue()).thenComparing(Entry::getKey));

    return offset.stream().map(Entry::getKey).toList();
  }

  private final OrebfuscatorConfig config;
  private final ObfuscationCache cache;
  private final ObfuscationStatistics statistics;

  private final List<BlockPos> offsets;

  public DeobfuscationWorker(OrebfuscatorCore orebfuscator) {
    this.config = orebfuscator.config();
    this.cache = orebfuscator.cache();
    this.statistics = orebfuscator.statistics().obfuscation;

    this.offsets = precomputeOffsets(orebfuscator.config().general().updateRadius());
  }

  public void deobfuscate(WorldAccessor world, @Nullable BlockPos block) {
    Objects.requireNonNull(world);
    if (block == null) {
      return;
    }

    deobfuscate(world, List.of(block));
  }

  public void deobfuscate(WorldAccessor world, @Nullable List<BlockPos> blocks) {
    Objects.requireNonNull(world);
    if (blocks == null || blocks.isEmpty()) {
      return;
    }

    ObfuscationConfig obfuscationConfig = world.config().obfuscation();
    if (obfuscationConfig == null || !obfuscationConfig.isEnabled()) {
      return;
    }

    var timer = statistics.debofuscation.start();
    try {
      this.deobfuscateNow(world, blocks);
    } finally {
      timer.stop();
    }
  }

  private void deobfuscateNow(WorldAccessor world, List<BlockPos> blocks) {
    final BlockFlags blockFlags = world.config().blockFlags();

    final Map<Long, ChunkAccessor> chunks = new HashMap<>();
    final Set<BlockPos> updatedBlocks = new HashSet<>();
    final Set<ChunkCacheKey> invalidChunks = new HashSet<>();

    for (BlockPos block : blocks) {
      for (var offset : offsets) {
        BlockPos position = block.add(offset);

        int chunkX = position.x() >> 4;
        int chunkZ = position.z() >> 4;

        long key = ChunkAccessor.chunkCoordsToLong(chunkX, chunkZ);
        ChunkAccessor chunk = chunks.computeIfAbsent(key, k -> world.getChunkNow(chunkX, chunkZ));
        if (ChunkAccessor.isNullOrEmpty(chunk)) {
          continue;
        }

        int blockState = chunk.getBlockState(position.x(), position.y(), position.z());
        if (!(BlockFlags.isObfuscateBitSet(blockFlags.flags(blockState)) && updatedBlocks.add(position))) {
          continue;
        }

        if (config.cache().enabled()) {
          ChunkCacheKey chunkPosition = new ChunkCacheKey(world, position);
          if (invalidChunks.add(chunkPosition)) {
            cache.invalidate(chunkPosition);
          }
        }
      }
    }

    world.sendBlockUpdates(updatedBlocks);
  }
}
