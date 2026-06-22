package net.imprex.orebfuscator.compatibility.spigot;

import net.imprex.orebfuscator.compatibility.CompatibilityScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class SpigotScheduler implements CompatibilityScheduler {

  private final Plugin plugin;

  public SpigotScheduler(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void runForPlayer(Player player, Runnable runnable) {
    if (this.plugin.isEnabled()) {
      Bukkit.getScheduler().runTask(this.plugin, runnable);
    }
  }

  @Override
  public void runAsyncNow(Runnable runnable) {
    if (this.plugin.isEnabled()) {
      Bukkit.getScheduler().runTaskAsynchronously(this.plugin, runnable);
    }
  }

  @Override
  public void cancelTasks() {
    Bukkit.getScheduler().cancelTasks(this.plugin);
  }
}
