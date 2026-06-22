package net.imprex.orebfuscator.compatibility.spigot;

import dev.imprex.orebfuscator.config.api.Config;
import net.imprex.orebfuscator.compatibility.CompatibilityLayer;
import net.imprex.orebfuscator.compatibility.CompatibilityScheduler;
import org.bukkit.plugin.Plugin;

public class SpigotCompatibilityLayer implements CompatibilityLayer {

  private final Thread mainThread = Thread.currentThread();

  private final SpigotScheduler scheduler;

  public SpigotCompatibilityLayer(Plugin plugin, Config config) {
    this.scheduler = new SpigotScheduler(plugin);
  }

  @Override
  public boolean isGameThread() {
    return Thread.currentThread() == this.mainThread;
  }

  @Override
  public CompatibilityScheduler getScheduler() {
    return this.scheduler;
  }
}
