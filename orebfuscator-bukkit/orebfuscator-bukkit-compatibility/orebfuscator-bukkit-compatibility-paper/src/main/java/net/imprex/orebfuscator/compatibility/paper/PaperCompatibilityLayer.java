package net.imprex.orebfuscator.compatibility.paper;

import dev.imprex.orebfuscator.config.api.Config;
import net.imprex.orebfuscator.compatibility.CompatibilityLayer;
import net.imprex.orebfuscator.compatibility.CompatibilityScheduler;
import net.imprex.orebfuscator.compatibility.spigot.SpigotScheduler;
import org.bukkit.plugin.Plugin;

public class PaperCompatibilityLayer implements CompatibilityLayer {

  private final Thread mainThread = Thread.currentThread();

  private final SpigotScheduler scheduler;

  public PaperCompatibilityLayer(Plugin plugin, Config config) {
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
