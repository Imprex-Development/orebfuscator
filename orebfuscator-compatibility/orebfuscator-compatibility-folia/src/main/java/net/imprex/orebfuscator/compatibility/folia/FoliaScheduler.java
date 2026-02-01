package net.imprex.orebfuscator.compatibility.folia;

import net.imprex.orebfuscator.compatibility.CompatibilityScheduler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class FoliaScheduler implements CompatibilityScheduler {

  private final Plugin plugin;

  public FoliaScheduler(Plugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public void runForPlayer(Player player, Runnable runnable) {
    if (this.plugin.isEnabled()) {
      player.getScheduler().run(this.plugin, task -> runnable.run(), null);
    }
  }

  @Override
  public void runAsyncNow(Runnable runnable) {
    if (this.plugin.isEnabled()) {
      Bukkit.getAsyncScheduler().runNow(this.plugin, task -> runnable.run());
    }
  }

  @Override
  public void cancelTasks() {
    Bukkit.getAsyncScheduler().cancelTasks(this.plugin);
  }
}
