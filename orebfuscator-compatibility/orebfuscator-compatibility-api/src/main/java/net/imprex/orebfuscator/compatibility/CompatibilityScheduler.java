package net.imprex.orebfuscator.compatibility;

import org.bukkit.entity.Player;

public interface CompatibilityScheduler {

  void runForPlayer(Player player, Runnable runnable);

  void runAsyncNow(Runnable runnable);

  void runAsyncAtFixedRate(Runnable runnable, long delay, long period);

  void cancelTasks();
}
