package net.imprex.orebfuscator.nms.v1_20_R3;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import dev.imprex.orebfuscator.cache.AbstractRegionFileCache;
import dev.imprex.orebfuscator.config.api.Config;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.util.BlockProperties;
import dev.imprex.orebfuscator.util.BlockStateProperties;
import dev.imprex.orebfuscator.util.BlockTag;
import dev.imprex.orebfuscator.util.NamespacedKey;
import it.unimi.dsi.fastutil.shorts.Short2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import net.imprex.orebfuscator.nms.AbstractNmsManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NmsManager extends AbstractNmsManager {

  private static final int BLOCK_ID_AIR = Block.getId(Blocks.AIR.defaultBlockState());

  static int getBlockState(ChunkAccess chunk, int x, int y, int z) {
    @Nullable LevelChunkSection[] sections = chunk.getSections();

    int sectionIndex = chunk.getSectionIndex(y);
    if (sectionIndex >= 0 && sectionIndex < sections.length) {
      LevelChunkSection section = sections[sectionIndex];
      if (section != null && !section.hasOnlyAir()) {
        return Block.getId(section.getBlockState(x & 0xF, y & 0xF, z & 0xF));
      }
    }

    return BLOCK_ID_AIR;
  }

  private static ServerLevel level(World world) {
    return worldHandle(world, ServerLevel.class);
  }

  private static ServerPlayer player(Player player) {
    return playerHandle(player, ServerPlayer.class);
  }

  public NmsManager() {
    super(Block.BLOCK_STATE_REGISTRY.size());

    for (Map.Entry<ResourceKey<Block>, Block> entry : BuiltInRegistries.BLOCK.entrySet()) {
      NamespacedKey namespacedKey = NamespacedKey.parse(entry.getKey().location().toString());
      Block block = entry.getValue();

      ImmutableList<BlockState> possibleBlockStates = block.getStateDefinition().getPossibleStates();
      BlockProperties.Builder builder = BlockProperties.builder(namespacedKey);

      for (BlockState blockState : possibleBlockStates) {
        BlockStateProperties properties = BlockStateProperties.builder(Block.getId(blockState))
            .withIsAir(blockState.isAir())
            .withIsFluid(!blockState.getFluidState().isEmpty())
            .withIsLava(block == Blocks.LAVA)
            .withIsOccluding(blockState.isSolidRender(EmptyBlockGetter.INSTANCE, BlockPos.ZERO))
            .withIsBlockEntity(blockState.hasBlockEntity())
            .withIsDefaultState(Objects.equals(block.defaultBlockState(), blockState))
            .build();

        builder.withBlockState(properties);
      }

      registerBlockProperties(builder.build());
    }

    BuiltInRegistries.BLOCK.getTags().map(Pair::getSecond).forEach(tag -> {
      NamespacedKey namespacedKey = NamespacedKey.parse(tag.key().location().toString());

      Set<BlockProperties> blocks = tag.stream()
          .map(holder -> holder.unwrapKey().map(key -> getBlockByName(key.location().toString())))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(Collectors.toUnmodifiableSet());

      registerBlockTag(new BlockTag(namespacedKey, blocks));
    });
  }

  @Override
  public AbstractRegionFileCache<?> createRegionFileCache(Config config) {
    return new RegionFileCache(config.cache());
  }

  @Override
  public CompletableFuture<@Nullable ChunkAccessor> getChunkFuture(World world, int chunkX, int chunkZ) {
    return level(world).getChunkSource()
        .getChunkFuture(chunkX, chunkZ, ChunkStatus.FULL, true)
        .thenApply(result -> {
          var chunk = result.left().orElse(null);
          return chunk != null ? new DefaultChunkAccessor(chunk) : null;
        });
  }

  @Override
  public @Nullable ChunkAccessor getChunkNow(World world, int chunkX, int chunkZ) {
    ServerChunkCache serverChunkCache = level(world).getChunkSource();

    LevelChunk chunk = serverChunkCache.getChunkNow(chunkX, chunkZ);
    if (chunk == null && serverChunkCache.isChunkLoaded(chunkX, chunkZ)) {
      chunk = serverChunkCache.getChunk(chunkX, chunkZ, false);
    }

    return chunk != null ? new DefaultChunkAccessor(chunk) : null;
  }

  @Override
  public void sendBlockUpdates(World world, Iterable<dev.imprex.orebfuscator.util.BlockPos> iterable) {
    ServerChunkCache serverChunkCache = level(world).getChunkSource();
    BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();

    for (dev.imprex.orebfuscator.util.BlockPos pos : iterable) {
      position.set(pos.x(), pos.y(), pos.z());
      serverChunkCache.blockChanged(position);
    }
  }

  @Override
  public void sendBlockUpdates(Player player, Iterable<dev.imprex.orebfuscator.util.BlockPos> iterable) {
    ServerPlayer serverPlayer = player(player);
    ServerLevel level = serverPlayer.serverLevel();
    ServerChunkCache serverChunkCache = level.getChunkSource();

    BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos();
    Map<SectionPos, Short2ObjectMap<BlockState>> sectionPackets = new HashMap<>();
    List<Packet<ClientGamePacketListener>> blockEntityPackets = new ArrayList<>();

    for (dev.imprex.orebfuscator.util.BlockPos pos : iterable) {
      if (!serverChunkCache.isChunkLoaded(pos.x() >> 4, pos.z() >> 4)) {
        continue;
      }

      position.set(pos.x(), pos.y(), pos.z());
      BlockState blockState = level.getBlockState(position);

      sectionPackets.computeIfAbsent(SectionPos.of(position), key -> new Short2ObjectLinkedOpenHashMap<>())
          .put(SectionPos.sectionRelativePos(position), blockState);

      if (blockState.hasBlockEntity()) {
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (blockEntity != null) {
          Packet<ClientGamePacketListener> packet = blockEntity.getUpdatePacket();
          if (packet != null) {
            blockEntityPackets.add(packet);
          }
        }
      }
    }

    for (Map.Entry<SectionPos, Short2ObjectMap<BlockState>> entry : sectionPackets.entrySet()) {
      Short2ObjectMap<BlockState> blockStates = entry.getValue();
      if (blockStates.size() == 1) {
        Short2ObjectMap.Entry<BlockState> blockEntry = blockStates.short2ObjectEntrySet().iterator().next();
        BlockPos blockPosition = entry.getKey().relativeToBlockPos(blockEntry.getShortKey());
        serverPlayer.connection.send(new ClientboundBlockUpdatePacket(blockPosition, blockEntry.getValue()));
      } else {
        serverPlayer.connection.send(new ClientboundSectionBlocksUpdatePacket(entry.getKey(),
            blockStates.keySet(), blockStates.values().toArray(BlockState[]::new)));
      }
    }

    for (Packet<ClientGamePacketListener> packet : blockEntityPackets) {
      serverPlayer.connection.send(packet);
    }
  }
}
