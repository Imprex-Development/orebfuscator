package dev.imprex.orebfuscator.obfuscation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.chunk.Chunk;
import dev.imprex.orebfuscator.chunk.ChunkFactory;
import dev.imprex.orebfuscator.chunk.ChunkSection;
import dev.imprex.orebfuscator.config.ProximityHeightCondition;
import dev.imprex.orebfuscator.config.api.BlockFlags;
import dev.imprex.orebfuscator.config.api.ObfuscationConfig;
import dev.imprex.orebfuscator.config.api.ProximityConfig;
import dev.imprex.orebfuscator.config.api.WorldConfigBundle;
import dev.imprex.orebfuscator.interop.ChunkPacketAccessor;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.player.ProximityBlock;
import dev.imprex.orebfuscator.statistics.OrebfuscatorStatistics;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.concurrent.OrebfuscatorThread;

@NullMarked
public class ObfuscationProcessor {

  private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);

  private final ChunkFactory chunkFactory;
  private final RegistryAccessor registryAccessor;
  private final OrebfuscatorStatistics statistics;

  public ObfuscationProcessor(OrebfuscatorCore orebfuscator) {
    this.chunkFactory = orebfuscator.chunkFactory();
    this.registryAccessor = orebfuscator.registry();
    this.statistics = orebfuscator.statistics();
  }

  public ObfuscationResponse process(ObfuscationRequest request) {
    var timer = statistics.injector.pipelineDelayProcessor.start();
    try {
      return processInternal(request);
    } finally {
      timer.stop();
    }
  }

  private ObfuscationResponse processInternal(ObfuscationRequest request) {
    ChunkPacketAccessor packet = request.packet();
    WorldAccessor worldAccessor = request.world();

    WorldConfigBundle bundle = worldAccessor.config();
    BlockFlags blockFlags = bundle.blockFlags();
    ObfuscationConfig obfuscationConfig = bundle.obfuscation();
    ProximityConfig proximityConfig = bundle.proximity();

    var state = STATE.get();

    Set<BlockPos> blockEntities = new HashSet<>();
    List<ProximityBlock> proximityBlocks = new ArrayList<>();

    RandomGenerator random = ThreadLocalRandom.current();
    if (Thread.currentThread() instanceof OrebfuscatorThread obfuscationThread) {
      random = obfuscationThread.random();
    }

    int baseX = packet.chunkX() << 4;
    int baseZ = packet.chunkZ() << 4;

    int layerY = Integer.MIN_VALUE;
    int layerYBlockState = -1;

    try (Chunk chunk = this.chunkFactory.fromPacket(request)) {
      for (int sectionIndex = Math.max(0, bundle.minSectionIndex()); sectionIndex <= Math
          .min(chunk.getSectionCount() - 1, bundle.maxSectionIndex()); sectionIndex++) {
        ChunkSection chunkSection = chunk.getSection(sectionIndex);
        if (chunkSection == null || chunkSection.isEmpty()) {
          continue;
        }

        final int baseY = worldAccessor.minBuildHeight() + (sectionIndex << 4);
        for (int index = 0; index < 4096; index++) {
          int y = baseY + (index >> 8 & 15);
          if (!bundle.shouldObfuscate(y)) {
            continue;
          }

          int blockState = chunkSection.getBlockState(index);

          int obfuscateBits = blockFlags.flags(blockState, y);
          if (BlockFlags.isEmpty(obfuscateBits)) {
            continue;
          }

          int x = baseX + (index & 15);
          int z = baseZ + (index >> 4 & 15);

          boolean isObfuscateBitSet = BlockFlags.isObfuscateBitSet(obfuscateBits);
          boolean obfuscated = false;

          // should current block be obfuscated
          if (isObfuscateBitSet && obfuscationConfig != null && obfuscationConfig.shouldObfuscate(y)
              && shouldObfuscate(request, chunk, state, x, y, z)) {
            if (state.isLava) {
              proximityBlocks.add(new ProximityBlock(new BlockPos(x, y, z), true));
            }
            if (obfuscationConfig.layerObfuscation()) {
              if (layerY != y) {
                layerY = y;
                layerYBlockState = bundle.nextRandomObfuscationBlock(random, y);
              }
              blockState = layerYBlockState;
            } else {
              blockState = bundle.nextRandomObfuscationBlock(random, y);
            }
            obfuscated = true;
          }

          // should current block be proximity hidden
          if (!obfuscated && BlockFlags.isProximityBitSet(obfuscateBits) && proximityConfig != null
              && proximityConfig.shouldObfuscate(y)) {
            proximityBlocks.add(new ProximityBlock(new BlockPos(x, y, z), false));
            if (BlockFlags.isUseBlockBelowBitSet(obfuscateBits)) {
              boolean allowNonOcclude = !isObfuscateBitSet || !ProximityHeightCondition.isPresent(obfuscateBits);
              blockState = getBlockStateBelow(random, bundle, chunk, x, y, z, allowNonOcclude);
            } else {
              blockState = bundle.nextRandomProximityBlock(random, y);
            }
            obfuscated = true;
          }

          // update block state if needed
          if (obfuscated) {
            chunkSection.setBlockState(index, blockState);
            if (BlockFlags.isBlockEntityBitSet(obfuscateBits)) {
              blockEntities.add(new BlockPos(x, y, z));
            }
          }

          state.reset();
        }
      }

      return new ObfuscationResponse(chunk.finalizeOutput(), blockEntities, proximityBlocks);
    }
  }

  // returns first block below given position that wouldn't be obfuscated in any
  // way at given position
  private int getBlockStateBelow(RandomGenerator random, WorldConfigBundle bundle, Chunk chunk, int x, int y, int z,
      boolean allowNonOcclude) {
    BlockFlags blockFlags = bundle.blockFlags();

    for (int targetY = y - 1; targetY > chunk.world().minBuildHeight(); targetY--) {
      int blockData = chunk.getBlockState(x, targetY, z);
      if (blockData != -1 && (allowNonOcclude || registryAccessor.isOccluding(blockData))) {
        int mask = blockFlags.flags(blockData, y);
        if (BlockFlags.isEmpty(mask) || BlockFlags.isAllowForUseBlockBelowBitSet(mask)) {
          return blockData;
        }
      }
    }

    return bundle.nextRandomProximityBlock(random, y);
  }

  private boolean shouldObfuscate(ObfuscationRequest request, Chunk chunk, State state, int x, int y, int z) {
    return isAdjacentBlockOccluding(request, chunk, state, x, y + 1, z)
        && isAdjacentBlockOccluding(request, chunk, state, x, y - 1, z)
        && isAdjacentBlockOccluding(request, chunk, state, x + 1, y, z)
        && isAdjacentBlockOccluding(request, chunk, state, x - 1, y, z)
        && isAdjacentBlockOccluding(request, chunk, state, x, y, z + 1)
        && isAdjacentBlockOccluding(request, chunk, state, x, y, z - 1);
  }

  private boolean isAdjacentBlockOccluding(ObfuscationRequest request, Chunk chunk, State state, int x, int y, int z) {
    int blockId = getBlockId(request, chunk, x, y, z);
    if (registryAccessor.isOccluding(blockId)) {
      return true;
    }

    if (registryAccessor.isLava(blockId)) {
      int aboveBlockId = getBlockId(request, chunk, x, y + 1, z);
      state.isLava = registryAccessor.isLava(aboveBlockId);
      return state.isLava;
    }

    return false;
  }

  private int getBlockId(ObfuscationRequest request, Chunk chunk, int x, int y, int z) {
    if (y >= chunk.world().maxBuildHeight() || y < chunk.world().minBuildHeight()) {
      return 0;
    }

    int blockId = chunk.getBlockState(x, y, z);
    if (blockId == -1) {
      blockId = request.getBlockState(x, y, z);
    }

    return blockId;
  }

  private static class State {

    public boolean isLava = false;

    public void reset() {
      this.isLava = false;
    }
  }
}