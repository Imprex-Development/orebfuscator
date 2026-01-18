package net.imprex.orebfuscator.iterop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;
import dev.imprex.orebfuscator.PermissionRequirements;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.interop.PlayerAccessor;
import dev.imprex.orebfuscator.player.OrebfuscatorPlayer;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.EntityPose;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorCompatibility;
import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.util.PermissionUtil;

// TODO: abstract static listener away into common management class, same thing for worlds as well
// TODO: Nullability
public class BukkitPlayerAccessor implements PlayerAccessor {

  private static final Map<UUID, BukkitPlayerAccessor> PLAYERS = new HashMap<>();

  public static void registerListener(Orebfuscator orebfuscator) {
    Bukkit.getPluginManager().registerEvents(new Listener() {
      @EventHandler
      public void onJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        var bukkitPlayer = PLAYERS.computeIfAbsent(player.getUniqueId(),
            key -> new BukkitPlayerAccessor(orebfuscator, player));
        bukkitPlayer.orebfuscatorPlayer.clearChunks();
      }

      @EventHandler
      public void onChangedWorld(PlayerChangedWorldEvent event) {
        var player = event.getPlayer();
        var bukkitPlayer = PLAYERS.get(player.getUniqueId());
        if (bukkitPlayer != null) {
          bukkitPlayer.world = BukkitWorldAccessor.get(player.getWorld());
          bukkitPlayer.orebfuscatorPlayer.clearChunks();
        }
      }
      
      @EventHandler
      public void onQuit(PlayerQuitEvent event) {
        PLAYERS.remove(event.getPlayer().getUniqueId());
      }
      
      @EventHandler
      public void onDisable(PluginDisableEvent event) {
        if (event.getPlugin() == orebfuscator) {
          PLAYERS.clear();
        }
      }
    }, orebfuscator);
    
    for (Player player : Bukkit.getOnlinePlayers()) {
      var bukkitPlayer = PLAYERS.computeIfAbsent(player.getUniqueId(),
          key -> new BukkitPlayerAccessor(orebfuscator, player));
      bukkitPlayer.orebfuscatorPlayer.clearChunks();
    }
  }

  @Nullable
  public static BukkitPlayerAccessor tryGet(Player player) {
    try {
      return PLAYERS.get(player.getUniqueId());
    } catch (UnsupportedOperationException e) {
      // catch TemporaryPlayer not implementing getUniqueId
      return null;
    }
  }

  public static List<BukkitPlayerAccessor> getAll() {
    return PLAYERS.values().stream().toList();
  }
  
  private final Player player;
  private BukkitWorldAccessor world;

  private final OrebfuscatorPlayer orebfuscatorPlayer;
  
  public BukkitPlayerAccessor(OrebfuscatorCore orebfuscator, Player player) {
    this.player = player;
    this.world = BukkitWorldAccessor.get(player.getWorld());
    this.orebfuscatorPlayer = new OrebfuscatorPlayer(orebfuscator, this);
  }

  @Override
  public OrebfuscatorPlayer orebfuscatorPlayer() {
    return this.orebfuscatorPlayer;
  }

  @Override
  public EntityPose pose() {
    var location = player.getLocation();
    return new EntityPose(world, location.getX(), location.getY(), location.getZ(), location.getPitch(), location.getYaw());
  }

  @Override
  public EntityPose eyePose() {
    var location = player.getEyeLocation();
    return new EntityPose(world, location.getX(), location.getY(), location.getZ(), location.getPitch(), location.getYaw());
  }

  @Override
  public BukkitWorldAccessor world() {
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
}
