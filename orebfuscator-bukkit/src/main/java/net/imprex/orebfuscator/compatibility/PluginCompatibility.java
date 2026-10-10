package net.imprex.orebfuscator.compatibility;

import dev.imprex.orebfuscator.logging.OfcLogger;
import net.imprex.orebfuscator.Orebfuscator;
import org.bukkit.Bukkit;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class PluginCompatibility {

  private @Nullable WorldEditListener worldEditListener;

  public void start(Orebfuscator orebfuscator) {
    if (Bukkit.getPluginManager().isPluginEnabled("WorldEdit")) {
      this.worldEditListener = new WorldEditListener(orebfuscator, orebfuscator.getObfuscationSystem());
      OfcLogger.debug("WorldEdit compatibility enabled");
    }
  }

  public void stop() {
    if (this.worldEditListener != null) {
      this.worldEditListener.unregister();
      this.worldEditListener = null;
    }
  }
}
