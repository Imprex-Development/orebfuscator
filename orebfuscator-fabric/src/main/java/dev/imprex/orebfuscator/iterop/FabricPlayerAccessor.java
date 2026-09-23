package dev.imprex.orebfuscator.iterop;

import dev.imprex.orebfuscator.PermissionRequirements;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.interop.PlayerAccessor;
import dev.imprex.orebfuscator.player.OrebfuscatorPlayer;
import dev.imprex.orebfuscator.reflect.Reflector;
import dev.imprex.orebfuscator.reflect.accessor.FieldAccessor;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.EntityPose;
import dev.imprex.orebfuscator.util.PermissionHelper;
import it.unimi.dsi.fastutil.shorts.Short2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.ShortSets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class FabricPlayerAccessor implements PlayerAccessor {

  private static final FieldAccessor MULTI_BLOCK_POSITIONS =
      Reflector.of(ClientboundSectionBlocksUpdatePacket.class).field().type().is(short[].class).firstOrThrow();
  private static final FieldAccessor MULTI_BLOCK_STATES =
      Reflector.of(ClientboundSectionBlocksUpdatePacket.class).field().type().is(BlockState[].class).firstOrThrow();

  private static final Map<UUID, FabricPlayerAccessor> PLAYERS = new HashMap<>();
  private static @Nullable LevelChunkSection emptySection;

  public static void registerListener(OrebfuscatorCore orebfuscator) {
    ServerPlayerEvents.JOIN.register((serverPlayer) -> {
      var fabricPlayer =
          PLAYERS.computeIfAbsent(serverPlayer.getUUID(), key -> new FabricPlayerAccessor(orebfuscator, serverPlayer));
      fabricPlayer.player = serverPlayer;
      fabricPlayer.level = FabricWorldAccessor.get(serverPlayer.level());
      fabricPlayer.orebfuscatorPlayer.clearChunks();
    });

    ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
      var fabricPlayer = PLAYERS.get(newPlayer.getUUID());
      if (fabricPlayer != null) {
        fabricPlayer.player = newPlayer;
        fabricPlayer.level = FabricWorldAccessor.get(newPlayer.level());
        fabricPlayer.orebfuscatorPlayer.clearChunks();
      }
    });

    ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, src, dst) -> {
      var fabricPlayer = PLAYERS.get(player.getUUID());
      if (fabricPlayer != null) {
        fabricPlayer.player = player;
        fabricPlayer.level = FabricWorldAccessor.get(dst);
        fabricPlayer.orebfuscatorPlayer.clearChunks();
      }
    });

    ServerPlayerEvents.LEAVE.register(player -> {
      var fabricPlayer = PLAYERS.remove(player.getUUID());
      if (fabricPlayer != null) {
        fabricPlayer.player = null;
      }
    });
  }

  @Nullable
  public static FabricPlayerAccessor tryGet(ServerPlayer player) {
    return PLAYERS.get(player.getUUID());
  }

  private static LevelChunkSection emptySection(ServerLevel level) {
    if (emptySection == null) {
      emptySection = new LevelChunkSection(level.palettedContainerFactory());
    }
    return emptySection;
  }

  public static List<FabricPlayerAccessor> getAll() {
    return PLAYERS.values().stream().toList();
  }

  private final MinecraftServer server;
  private final UUID uuid;

  private final OrebfuscatorPlayer orebfuscatorPlayer;

  private @Nullable ServerPlayer player;
  private FabricWorldAccessor level;

  public FabricPlayerAccessor(OrebfuscatorCore orebfuscator, ServerPlayer player) {
    this.server = player.level().getServer();
    this.uuid = player.getUUID();

    this.orebfuscatorPlayer = new OrebfuscatorPlayer(orebfuscator, this);
  }

  @Override
  public OrebfuscatorPlayer orebfuscatorPlayer() {
    return orebfuscatorPlayer;
  }

  @Override
  public EntityPose pose() {
    if (player == null) {
      return EntityPose.ZERO;
    }

    return new EntityPose(
        level,
        player.position().x,
        player.position().y,
        player.position().z,
        player.getXRot(),
        player.getYRot());
  }

  @Override
  public EntityPose eyePose() {
    if (player == null) {
      return EntityPose.ZERO;
    }

    return new EntityPose(
        level,
        player.position().x,
        player.getEyeY(),
        player.position().z,
        player.getXRot(),
        player.getYRot());
  }

  @Override
  public FabricWorldAccessor world() {
    return level;
  }

  @Override
  public boolean isAlive() {
    return player != null && player.isAlive();
  }

  @Override
  public boolean isSpectator() {
    return player != null && player.isSpectator();
  }

  @Override
  public double lavaFogDistance() {
    return player != null && player.hasEffect(MobEffects.FIRE_RESISTANCE) ? 7 : 2;
  }

  @Override
  public boolean hasPermission(PermissionRequirements requirements) {
    if (this.player == null) {
      return false;
    }

    return PermissionHelper.has(this.player, requirements);
  }

  @Override
  public void runForPlayer(Runnable runnable) {
    if (!server.isStopped()) {
      server.executeIfPossible(runnable);
    }
  }

  @Override
  public void sendBlockUpdates(Iterable<BlockPos> iterable) {
    if (player == null) {
      return;
    }

    ServerLevel level = player.level();
    ServerChunkCache serverChunkCache = level.getChunkSource();

    net.minecraft.core.BlockPos.MutableBlockPos position = new net.minecraft.core.BlockPos.MutableBlockPos();
    Map<SectionPos, Short2ObjectMap<BlockState>> sectionPackets = new HashMap<>();
    List<Packet<ClientGamePacketListener>> blockEntityPackets = new ArrayList<>();

    for (dev.imprex.orebfuscator.util.BlockPos pos : iterable) {
      if (!serverChunkCache.hasChunk(pos.x() >> 4, pos.z() >> 4)) {
        continue;
      }

      position.set(pos.x(), pos.y(), pos.z());
      BlockState blockState = level.getBlockState(position);

      sectionPackets.computeIfAbsent(SectionPos.of(position), key -> new Short2ObjectLinkedOpenHashMap<>())
          .put(SectionPos.sectionRelativePos(position), blockState);

      if (blockState.hasBlockEntity()) {
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (blockEntity != null) {
          var updatePacket = blockEntity.getUpdatePacket();
          if (updatePacket != null) {
            blockEntityPackets.add(updatePacket);
          }
        }
      }
    }

    for (Map.Entry<SectionPos, Short2ObjectMap<BlockState>> entry : sectionPackets.entrySet()) {
      Short2ObjectMap<BlockState> blockStates = entry.getValue();
      if (blockStates.size() == 1) {
        Short2ObjectMap.Entry<BlockState> blockEntry = blockStates.short2ObjectEntrySet().iterator().next();
        net.minecraft.core.BlockPos blockPosition = entry.getKey().relativeToBlockPos(blockEntry.getShortKey());
        player.connection.send(new ClientboundBlockUpdatePacket(blockPosition, blockEntry.getValue()));
      } else {
        var packet =
            new ClientboundSectionBlocksUpdatePacket(entry.getKey(), ShortSets.emptySet(), emptySection(level));
        MULTI_BLOCK_POSITIONS.set(packet, blockStates.keySet().toShortArray());
        MULTI_BLOCK_STATES.set(packet, blockStates.values().toArray(BlockState[]::new));
        player.connection.send(packet);
      }
    }

    for (Packet<ClientGamePacketListener> packet : blockEntityPackets) {
      player.connection.send(packet);
    }
  }

  @Override
  public String toString() {
    return "FabricPlayerAccessor{" + "uuid=" + uuid + ", player=" + player + ", level=" + level + '}';
  }
}
