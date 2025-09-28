package net.imprex.orebfuscator.nms.v1_16_R2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_16_R2.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.comphenix.protocol.events.PacketContainer;
import com.google.common.collect.ImmutableList;

import net.imprex.orebfuscator.config.Config;
import net.imprex.orebfuscator.nms.AbstractNmsManager;
import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.BlockProperties;
import net.imprex.orebfuscator.util.BlockStateProperties;
import net.imprex.orebfuscator.util.NamespacedKey;
import net.minecraft.server.v1_16_R2.Block;
import net.minecraft.server.v1_16_R2.BlockPosition;
import net.minecraft.server.v1_16_R2.Blocks;
import net.minecraft.server.v1_16_R2.Chunk;
import net.minecraft.server.v1_16_R2.ChunkProviderServer;
import net.minecraft.server.v1_16_R2.ChunkSection;
import net.minecraft.server.v1_16_R2.EntityPlayer;
import net.minecraft.server.v1_16_R2.IBlockData;
import net.minecraft.server.v1_16_R2.IRegistry;
import net.minecraft.server.v1_16_R2.Packet;
import net.minecraft.server.v1_16_R2.PacketListenerPlayOut;
import net.minecraft.server.v1_16_R2.PacketPlayOutBlockChange;
import net.minecraft.server.v1_16_R2.PacketPlayOutMultiBlockChange;
import net.minecraft.server.v1_16_R2.ResourceKey;
import net.minecraft.server.v1_16_R2.SectionPosition;
import net.minecraft.server.v1_16_R2.TileEntity;
import net.minecraft.server.v1_16_R2.WorldServer;

public class NmsManager extends AbstractNmsManager {

  private static final int BLOCK_ID_AIR = Block.getCombinedId(Blocks.AIR.getBlockData());

  static int getBlockState(Chunk chunk, int x, int y, int z) {
    ChunkSection[] sections = chunk.getSections();

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
    return ((CraftWorld) world).getHandle();
  }

  private static EntityPlayer player(Player player) {
    return ((CraftPlayer) player).getHandle();
  }

  public NmsManager(Config config) {
    super(Block.REGISTRY_ID.a(), new RegionFileCache(config.cache()));

    for (Map.Entry<ResourceKey<Block>, Block> entry : IRegistry.BLOCK.d()) {
      NamespacedKey namespacedKey = NamespacedKey.fromString(entry.getKey().a().toString());
      Block block = entry.getValue();

      ImmutableList<IBlockData> possibleBlockStates = block.getStates().a();
      BlockProperties.Builder builder = BlockProperties.builder(namespacedKey);

      for (IBlockData blockState : possibleBlockStates) {
        Material material = CraftBlockData.fromData(blockState).getMaterial();

        BlockStateProperties properties = BlockStateProperties.builder(Block.getCombinedId(blockState))
            .withIsAir(blockState.isAir())
            /**
             * l -> for barrier/slime_block/spawner/leaves
             * isOccluding -> for every other block
             */
            .withIsOccluding(material.isOccluding() && blockState.l()/*canOcclude*/)
            .withIsBlockEntity(block.isTileEntity())
            .withIsDefaultState(Objects.equals(block.getBlockData(), blockState))
            .build();

        builder.withBlockState(properties);
      }

      this.registerBlockProperties(builder.build());
    }
  }

  @Override
  public ReadOnlyChunk getReadOnlyChunk(World world, int chunkX, int chunkZ) {
    ChunkProviderServer chunkProviderServer = level(world).getChunkProvider();
    Chunk chunk = chunkProviderServer.getChunkAt(chunkX, chunkZ, true);
    return new ReadOnlyChunkWrapper(chunk);
  }

  @Override
  public int getBlockState(World world, int x, int y, int z) {
    ChunkProviderServer serverChunkCache = level(world).getChunkProvider();
    if (!serverChunkCache.isChunkLoaded(x >> 4, z >> 4)) {
      return BLOCK_ID_AIR;
    }

    Chunk chunk = serverChunkCache.getChunkAt(x >> 4, z >> 4, true);
    if (chunk == null) {
      return BLOCK_ID_AIR;
    }

    return getBlockState(chunk, x, y, z);
  }

  @Override
  public void sendBlockUpdates(World world, Iterable<net.imprex.orebfuscator.util.BlockPos> iterable) {
    ChunkProviderServer serverChunkCache = level(world).getChunkProvider();
    BlockPosition.MutableBlockPosition position = new BlockPosition.MutableBlockPosition();

    for (net.imprex.orebfuscator.util.BlockPos pos : iterable) {
      position.c(pos.x, pos.y, pos.z);
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

    for (net.imprex.orebfuscator.util.BlockPos pos : iterable) {
      if (!serverChunkCache.isChunkLoaded(pos.x >> 4, pos.z >> 4)) {
        continue;
      }

      position.c(pos.x, pos.y, pos.z);
      IBlockData blockState = level.getType(position);

      sectionPackets.computeIfAbsent(SectionPosition.a(position), key -> new HashMap<>())
          .put(SectionPosition.b(position), blockState);

      if (blockState.getBlock().isTileEntity()) {
        TileEntity blockEntity = level.getTileEntity(position);
        if (blockEntity != null) {
          blockEntityPackets.add(blockEntity.getUpdatePacket());
        }
      }
    }

    for (Map.Entry<SectionPosition, Map<Short, IBlockData>> entry : sectionPackets.entrySet()) {
      Map<Short, IBlockData> blockStates = entry.getValue();
      if (blockStates.size() == 1) {
        Map.Entry<Short, IBlockData> blockEntry = blockStates.entrySet().iterator().next();
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
