package dev.imprex.orebfuscator.iterop;

import dev.imprex.orebfuscator.Orebfuscator;
import dev.imprex.orebfuscator.config.api.WorldConfigBundle;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.interop.ChunkPacketAccessor;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import dev.imprex.orebfuscator.util.ChunkDirection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class FabricWorldAccessor implements WorldAccessor {

  private static final int BLOCK_ID_AIR = Block.getId(Blocks.AIR.defaultBlockState());

  static int getBlockState(ChunkAccess chunk, int x, int y, int z) {
    LevelChunkSection[] sections = chunk.getSections();

    int sectionIndex = chunk.getSectionIndex(y);
    if (sectionIndex >= 0 && sectionIndex < sections.length) {
      LevelChunkSection section = sections[sectionIndex];
      if (section != null && !section.hasOnlyAir()) {
        return Block.getId(section.getBlockState(x & 0xF, y & 0xF, z & 0xF));
      }
    }

    return BLOCK_ID_AIR;
  }

  private static final Map<ServerLevel, FabricWorldAccessor> ACCESSOR_LOOKUP = new ConcurrentHashMap<>();

  public static FabricWorldAccessor get(ServerLevel level) {
    return ACCESSOR_LOOKUP.computeIfAbsent(level, key -> {
      throw new IllegalStateException("Created world accessor outside of event!");
    });
  }

  private static int blockToSectionCoord(int block) {
    return block >> 4;
  }


  public static Collection<FabricWorldAccessor> getLevels() {
    return ACCESSOR_LOOKUP.values();
  }

  public static void registerListener(MinecraftServer server, Orebfuscator orebfuscator) {
    ServerLevelEvents.UNLOAD.register((s, level) -> {
      ACCESSOR_LOOKUP.remove(level);
    });

    for (ServerLevel level : server.getAllLevels()) {
      ACCESSOR_LOOKUP.put(level, new FabricWorldAccessor(level, orebfuscator));
    }

    ServerLevelEvents.LOAD.register((s, level) -> {
      ACCESSOR_LOOKUP.put(level, new FabricWorldAccessor(level, orebfuscator));
    });
  }

  public final ServerLevel level;
  private final Orebfuscator orebfuscator;

  private final int maxHeight;
  private final int minHeight;

  private @Nullable WorldConfigBundle worldConfigBundle;

  private FabricWorldAccessor(ServerLevel level, Orebfuscator orebfuscator) {
    this.level = Objects.requireNonNull(level);
    this.orebfuscator = Objects.requireNonNull(orebfuscator);

    this.maxHeight = level.getMaxY();
    this.minHeight = level.getMinY();
  }

  @Override
  public WorldConfigBundle config() {
    if (this.worldConfigBundle == null) {
      this.worldConfigBundle = this.orebfuscator.config().world(this);
    }
    return this.worldConfigBundle;
  }

  @Override
  public String name() {
    return this.level.dimension().identifier().toString();
  }

  @Override
  public int height() {
    return this.maxHeight - this.minHeight;
  }

  @Override
  public int minBuildHeight() {
    return this.minHeight;
  }

  @Override
  public int maxBuildHeight() {
    return this.maxHeight;
  }

  @Override
  public int sectionCount() {
    return this.maxSection() - this.minSection();
  }

  @Override
  public int minSection() {
    return blockToSectionCoord(this.minBuildHeight());
  }

  @Override
  public int maxSection() {
    return blockToSectionCoord(this.maxBuildHeight() - 1) + 1;
  }

  @Override
  public int sectionIndex(int y) {
    return blockToSectionCoord(y) - minSection();
  }

  @Override
  public ChunkAccessor getChunkNow(int chunkX, int chunkZ) {
    ServerChunkCache serverChunkCache = level.getChunkSource();

    LevelChunk chunk = serverChunkCache.getChunkNow(chunkX, chunkZ);
    if (chunk == null) {
      var key = ChunkPos.pack(chunkX, chunkZ);
      var chunkHolder = serverChunkCache.chunkMap.getUpdatingChunkIfPresent(key);
      if (chunkHolder != null) {
        chunk = chunkHolder.getFullChunkFuture().getNow(ChunkHolder.UNLOADED_LEVEL_CHUNK).orElse(null);
      }
    }

    return chunk != null ? new FabricChunkAccessor(chunk) : ChunkAccessor.EMPTY;
  }


  public ChunkAccessor[] getNeighboringChunksNow(int chunkX, int chunkZ) {
    ServerChunkCache serverChunkCache = level.getChunkSource();
    ChunkAccessor[] neighboringChunks = new ChunkAccessor[4];

    for (ChunkDirection direction : ChunkDirection.values()) {
      int x = chunkX + direction.getOffsetX();
      int z = chunkZ + direction.getOffsetZ();
      int index = direction.ordinal();

      var chunk = serverChunkCache.getChunkNow(x, z);
      if (chunk != null) {
        neighboringChunks[index] = new FabricChunkAccessor(chunk);
      } else {
        neighboringChunks[index] = ChunkAccessor.EMPTY;
      }
    }

    return neighboringChunks;
  }

  @Override
  @SuppressWarnings("unchecked")
  public CompletableFuture<ChunkAccessor[]> getNeighboringChunks(ObfuscationRequest request) {
    var neighborChunks = request.neighborChunks();
    if (neighborChunks != null && Arrays.stream(neighborChunks).noneMatch(ChunkAccessor::isNullOrEmpty)) {
      return CompletableFuture.completedFuture(neighborChunks);
    }

    final ChunkPacketAccessor packet = request.packet();
    ServerChunkCache serverChunkCache = level.getChunkSource();
    final CompletableFuture<ChunkAccessor>[] futures = (CompletableFuture<ChunkAccessor>[]) new CompletableFuture[4];

    for (ChunkDirection direction : ChunkDirection.values()) {
      int chunkX = packet.chunkX() + direction.getOffsetX();
      int chunkZ = packet.chunkZ() + direction.getOffsetZ();

      int index = direction.ordinal();
      var chunk = neighborChunks != null ? neighborChunks[index] : null;

      if (ChunkAccessor.isNullOrEmpty(chunk)) {
        futures[index] =
            serverChunkCache.getChunkFuture(chunkX, chunkZ, ChunkStatus.FULL, true).thenApply(chunkResult -> {
              var value = chunkResult.orElse(null);
              return value != null ? new FabricChunkAccessor(value) : ChunkAccessor.EMPTY;
            });
      } else {
        futures[index] = CompletableFuture.completedFuture(chunk);
      }
    }

    return CompletableFuture.allOf(futures)
        .thenApply(v -> Arrays.stream(futures).map(CompletableFuture::join).toArray(ChunkAccessor[]::new));
  }

  @Override
  public void sendBlockUpdates(Iterable<dev.imprex.orebfuscator.util.BlockPos> iterable) {
    ServerChunkCache serverChunkCache = level.getChunkSource();
    BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

    for (dev.imprex.orebfuscator.util.BlockPos pos : iterable) {
      position.set(pos.x(), pos.y(), pos.z());
      serverChunkCache.blockChanged(position);
    }
  }

  @Override
  public int hashCode() {
    return this.level.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    return obj instanceof FabricWorldAccessor other && this.level.equals(other.level);
  }

  @Override
  public String toString() {
    return String.format("[%s, minY=%s, maxY=%s]", level.dimension(), minHeight, maxHeight);
  }
}
