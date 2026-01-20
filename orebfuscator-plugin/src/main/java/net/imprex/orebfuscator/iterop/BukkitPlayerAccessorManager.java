package net.imprex.orebfuscator.iterop;

import dev.imprex.orebfuscator.interop.PlayerAccessor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.imprex.orebfuscator.Orebfuscator;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginDisableEvent;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class BukkitPlayerAccessorManager implements Listener {

  private final Map<Player, BukkitPlayerAccessor> players = new HashMap<>();

  private final Orebfuscator orebfuscator;
  private final BukkitWorldAccessorManager worldManager;

  public BukkitPlayerAccessorManager(Orebfuscator orebfuscator) {
    this.orebfuscator = orebfuscator;
    this.worldManager = orebfuscator.worldManager();

    Bukkit.getPluginManager().registerEvents(this, orebfuscator);

    for (Player player : Bukkit.getOnlinePlayers()) {
      this.players.computeIfAbsent(player, key -> new BukkitPlayerAccessor(orebfuscator, key))
          .orebfuscatorPlayer().clearChunks();
    }
  }

  public List<PlayerAccessor> all() {
    return this.players.values().stream()
        .map(PlayerAccessor.class::cast)
        .toList();
  }

  public @Nullable BukkitPlayerAccessor tryGet(Player player) {
    return this.players.get(player);
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    this.players.computeIfAbsent(event.getPlayer(), key -> new BukkitPlayerAccessor(orebfuscator, key))
        .orebfuscatorPlayer().clearChunks();
  }

  @EventHandler
  public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
    var player = event.getPlayer();
    var bukkitPlayer = this.players.get(player);
    if (bukkitPlayer != null) {
      bukkitPlayer.changeWorld(this.worldManager.get(player.getWorld()));
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    this.players.remove(event.getPlayer());
  }

  @EventHandler
  public void onDisable(PluginDisableEvent event) {
    if (event.getPlugin() == this.orebfuscator) {
      this.players.clear();
    }
  }
}
