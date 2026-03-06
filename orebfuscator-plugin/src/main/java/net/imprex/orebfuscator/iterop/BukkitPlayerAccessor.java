package net.imprex.orebfuscator.iterop;

import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.events.PacketEvent;
import dev.imprex.orebfuscator.PermissionRequirements;
import dev.imprex.orebfuscator.interop.PlayerAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.player.OrebfuscatorPlayer;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.EntityPose;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorCompatibility;
import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.obfuscation.PendingChunkBatch;
import net.imprex.orebfuscator.util.PermissionUtil;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BukkitPlayerAccessor implements PlayerAccessor {

  private final Orebfuscator orebfuscator;
  private final Player player;
  private BukkitWorldAccessor world;

  private final BukkitWorldAccessorManager worldManager;
  private final OrebfuscatorPlayer orebfuscatorPlayer;

  private final Map<Object, CompletableFuture<Void>> pendingPackets = new WeakHashMap<>();
  private final AtomicReference<@Nullable PendingChunkBatch> chunkBatch = new AtomicReference<>();
  private volatile boolean respawning = false;

  public BukkitPlayerAccessor(Orebfuscator orebfuscator, Player player) {
    this.orebfuscator = orebfuscator;
    this.player = player;
    this.worldManager = orebfuscator.worldManager();
    this.world = this.worldManager.get(player.getWorld());
    this.orebfuscatorPlayer = new OrebfuscatorPlayer(orebfuscator, this);
  }

  public void startRespawn() {
    this.respawning = true;

    this.runForPlayer(() -> {
      this.respawning = false;
    });
  }

  public boolean isRespawning() {
    return this.respawning;
  }

  public void changeWorld(BukkitWorldAccessor world) {
    this.world = world;
    this.orebfuscatorPlayer.clearChunks();
  }

  public @Nullable CompletableFuture<Void> obfuscationFuture(PacketEvent event) {
    return pendingPackets.remove(event.getPacket().getHandle());
  }

  public void obfuscationFuture(PacketEvent event, CompletableFuture<Void> future) {
    pendingPackets.putIfAbsent(event.getPacket().getHandle(), future);
  }

  public void startBatch(AsynchronousManager asynchronousManager, PacketEvent event) {
    var nextBatch = new PendingChunkBatch(this.orebfuscator.statistics().injector, asynchronousManager, event);
    var prevBatch = this.chunkBatch.getAndSet(nextBatch);

    if (prevBatch != null) {
      prevBatch.finish();
      OfcLogger.warn("Pending chunk batch discarded because a new batch was initiated.");
    }
  }

  public boolean addBatchChunk(CompletableFuture<Void> future) {
    var batch = this.chunkBatch.get();
    if (batch != null) {
      batch.addChunk(future);
      return true;
    }
    return false;
  }

  public void finishBatch() {
    var batch = this.chunkBatch.getAndSet(null);
    if (batch != null) {
      batch.finish();
    }
  }

  @Override
  public OrebfuscatorPlayer orebfuscatorPlayer() {
    return this.orebfuscatorPlayer;
  }

  @Override
  public EntityPose pose() {
    var location = player.getLocation();
    return new EntityPose(world, location.getX(), location.getY(), location.getZ(), location.getPitch(),
        location.getYaw());
  }

  @Override
  public EntityPose eyePose() {
    var location = player.getEyeLocation();
    return new EntityPose(world, location.getX(), location.getY(), location.getZ(), location.getPitch(),
        location.getYaw());
  }

  @Override
  public BukkitWorldAccessor world() {
    World bukkitWorld = player.getWorld();

    if (!Objects.equals(this.world.world, bukkitWorld)) {
      this.changeWorld(this.worldManager.get(bukkitWorld));
    }

    return world;
  }

  @Override
  public boolean isAlive() {
    return !player.isDead();
  }

  @Override
  public boolean isSpectator() {
    return player.getGameMode() == GameMode.SPECTATOR;
  }

  @Override
  public double lavaFogDistance() {
    return player.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE) ? 7 : 2;
  }

  @Override
  public boolean hasPermission(PermissionRequirements permission) {
    return PermissionUtil.hasPermission(player, permission);
  }

  @Override
  public void runForPlayer(Runnable runnable) {
    OrebfuscatorCompatibility.runForPlayer(player, runnable);
  }

  @Override
  public void sendBlockUpdates(Iterable<BlockPos> iterable) {
    OrebfuscatorNms.sendBlockUpdates(player, iterable);
  }

  @Override
  public String toString() {
    return this.player.toString();
  }
}
