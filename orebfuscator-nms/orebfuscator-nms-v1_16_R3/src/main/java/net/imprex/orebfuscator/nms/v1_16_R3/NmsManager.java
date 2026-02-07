package net.imprex.orebfuscator.nms.v1_16_R3;

import com.comphenix.protocol.events.PacketContainer;
import com.google.common.collect.ImmutableList;
import dev.imprex.orebfuscator.cache.AbstractRegionFileCache;
import dev.imprex.orebfuscator.config.api.Config;
import dev.imprex.orebfuscator.interop.ChunkAccessor;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.BlockProperties;
import dev.imprex.orebfuscator.util.BlockStateProperties;
import dev.imprex.orebfuscator.util.BlockTag;
import dev.imprex.orebfuscator.util.NamespacedKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import net.imprex.orebfuscator.nms.AbstractNmsManager;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockAccessAir;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.Blocks;
import net.minecraft.server.v1_16_R3.Chunk;
import net.minecraft.server.v1_16_R3.ChunkProviderServer;
import net.minecraft.server.v1_16_R3.ChunkSection;
import net.minecraft.server.v1_16_R3.EntityPlayer;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.IRegistry;
import net.minecraft.server.v1_16_R3.MinecraftKey;
import net.minecraft.server.v1_16_R3.Packet;
import net.minecraft.server.v1_16_R3.PacketListenerPlayOut;
import net.minecraft.server.v1_16_R3.PacketPlayOutBlockChange;
import net.minecraft.server.v1_16_R3.PacketPlayOutMultiBlockChange;
import net.minecraft.server.v1_16_R3.ResourceKey;
import net.minecraft.server.v1_16_R3.SectionPosition;
import net.minecraft.server.v1_16_R3.Tag;
import net.minecraft.server.v1_16_R3.TagsBlock;
import net.minecraft.server.v1_16_R3.TileEntity;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class NmsManager extends AbstractNmsManager {

  private static final int BLOCK_ID_AIR = Block.getCombinedId(Blocks.AIR.getBlockData());

  static int getBlockState(Chunk chunk, int x, int y, int z) {
    @Nullable ChunkSection[] sections = chunk.getSections();

    int sectionIndex = y >> 4;
    if (sectionIndex >= 0 && sectionIndex < sections.length) {
      ChunkSection section = sections[sectionIndex];
      if (section != null && !ChunkSection.a(section)) {
        return Block.getCombinedId(section.getType(x & 0xF, y & 0xF, z & 0xF));
      }
    }

    return BLOCK_ID_AIR;
  }

  private static WorldServer level(World world) {
    return worldHandle(world, WorldServer.class);
  }

  private static EntityPlayer player(Player player) {
    return playerHandle(player, EntityPlayer.class);
  }

  public NmsManager() {
    super(Block.REGISTRY_ID.a());

    for (Entry<ResourceKey<Block>, Block> entry : IRegistry.BLOCK.d()) {
      NamespacedKey namespacedKey = NamespacedKey.parse(entry.getKey().a().toString());
      Block block = entry.getValue();

      ImmutableList<IBlockData> possibleBlockStates = block.getStates().a();
      BlockProperties.Builder builder = BlockProperties.builder(namespacedKey);

      for (IBlockData blockState : possibleBlockStates) {
        BlockStateProperties properties = BlockStateProperties.builder(Block.getCombinedId(blockState))
            .withIsAir(blockState.isAir())
            .withIsLava(block == Blocks.LAVA)
            .withIsOccluding(blockState.i(BlockAccessAir.INSTANCE, BlockPosition.ZERO)/*isSolidRender*/)
            .withIsBlockEntity(block.isTileEntity())
            .withIsDefaultState(Objects.equals(block.getBlockData(), blockState))
            .build();

        builder.withBlockState(properties);
      }

      registerBlockProperties(builder.build());
    }

    for (Entry<MinecraftKey, Tag<Block>> entry : TagsBlock.a().a().entrySet()) {
      NamespacedKey namespacedKey = NamespacedKey.parse(entry.getKey().toString());

      Set<BlockProperties> blocks = new HashSet<>();
      for (Block block : entry.getValue().getTagged()) {
        BlockProperties properties = getBlockByName(IRegistry.BLOCK.getKey(block).toString());
        if (properties != null) {
          blocks.add(properties);
        }
      }

      registerBlockTag(new BlockTag(namespacedKey, blocks));
    }
  }

  @Override
  public AbstractRegionFileCache<?> createRegionFileCache(Config config) {
    return new RegionFileCache(config.cache());
  }

  @Override
  public CompletableFuture<@Nullable ChunkAccessor> getChunkFuture(World world, int chunkX, int chunkZ) {
    Chunk chunk = level(world).getChunkProvider().getChunkAt(chunkX, chunkZ, true);
    return CompletableFuture.completedFuture(chunk != null ? new DefaultChunkAccessor(chunk) : null);
  }

  @Override
  public @Nullable ChunkAccessor getChunkNow(World world, int chunkX, int chunkZ) {
    ChunkProviderServer serverChunkCache = level(world).getChunkProvider();

    Chunk chunk = serverChunkCache.a(chunkX, chunkZ);
    if (chunk == null && serverChunkCache.isChunkLoaded(chunkX, chunkZ)) {
      chunk = serverChunkCache.getChunkAt(chunkX, chunkZ, false);
    }

    return chunk != null ? new DefaultChunkAccessor(chunk) : null;
  }

  @Override
  public void sendBlockUpdates(World world, Iterable<BlockPos> iterable) {
    ChunkProviderServer serverChunkCache = level(world).getChunkProvider();
    BlockPosition.MutableBlockPosition position = new BlockPosition.MutableBlockPosition();

    for (BlockPos pos : iterable) {
      position.c(pos.x(), pos.y(), pos.z());
      serverChunkCache.flagDirty(position);
    }
  }

  @Override
  public void sendBlockUpdates(Player player, Iterable<BlockPos> iterable) {
    EntityPlayer serverPlayer = player(player);
    WorldServer level = serverPlayer.getWorldServer();
    ChunkProviderServer serverChunkCache = level.getChunkProvider();

    BlockPosition.MutableBlockPosition position = new BlockPosition.MutableBlockPosition();
    Map<SectionPosition, Map<Short, IBlockData>> sectionPackets = new HashMap<>();
    List<Packet<PacketListenerPlayOut>> blockEntityPackets = new ArrayList<>();

    for (BlockPos pos : iterable) {
      if (!serverChunkCache.isChunkLoaded(pos.x() >> 4, pos.z() >> 4)) {
        continue;
      }

      position.c(pos.x(), pos.y(), pos.z());
      IBlockData blockState = level.getType(position);

      sectionPackets.computeIfAbsent(SectionPosition.a(position), key -> new HashMap<>())
          .put(SectionPosition.b(position), blockState);

      if (blockState.getBlock().isTileEntity()) {
        TileEntity blockEntity = level.getTileEntity(position);
        if (blockEntity != null) {
          Packet<PacketListenerPlayOut> packet = blockEntity.getUpdatePacket();
          if (packet != null) {
            blockEntityPackets.add(packet);
          }
        }
      }
    }

    for (Entry<SectionPosition, Map<Short, IBlockData>> entry : sectionPackets.entrySet()) {
      Map<Short, IBlockData> blockStates = entry.getValue();
      if (blockStates.size() == 1) {
        Entry<Short, IBlockData> blockEntry = blockStates.entrySet().iterator().next();
        BlockPosition blockPosition = entry.getKey().g(blockEntry.getKey());
        serverPlayer.playerConnection.sendPacket(new PacketPlayOutBlockChange(blockPosition, blockEntry.getValue()));
      } else {
        // fix #324: use empty constructor cause ChunkSection can only be null for spigot forks
        PacketContainer packet = PacketContainer.fromPacket(new PacketPlayOutMultiBlockChange());
        packet.getSpecificModifier(SectionPosition.class).write(0, entry.getKey());
        packet.getSpecificModifier(short[].class).write(0, toShortArray(blockStates.keySet()));
        packet.getSpecificModifier(IBlockData[].class).write(0, blockStates.values().toArray(IBlockData[]::new));
        serverPlayer.playerConnection.sendPacket((Packet<?>) packet.getHandle());
      }
    }

    for (Packet<PacketListenerPlayOut> packet : blockEntityPackets) {
      serverPlayer.playerConnection.sendPacket(packet);
    }
  }

  private static short[] toShortArray(Set<Short> set) {
    short[] array = new short[set.size()];

    int i = 0;
    for (Short value : set) {
      array[i++] = value;
    }

    return array;
  }
}
