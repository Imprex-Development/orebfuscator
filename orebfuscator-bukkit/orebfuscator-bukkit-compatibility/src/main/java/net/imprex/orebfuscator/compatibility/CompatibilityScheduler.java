package net.imprex.orebfuscator.compatibility;

import org.bukkit.entity.Player;

public interface CompatibilityScheduler {

  void runForPlayer(Player player, Runnable runnable);

  void runAsyncNow(Runnable runnable);

  void cancelTasks();
}
