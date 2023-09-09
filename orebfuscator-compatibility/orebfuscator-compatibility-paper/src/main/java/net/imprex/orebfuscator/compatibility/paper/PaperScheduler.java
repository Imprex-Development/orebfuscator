package net.imprex.orebfuscator.compatibility.paper;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.imprex.orebfuscator.compatibility.CompatibilityScheduler;

public class PaperScheduler implements CompatibilityScheduler {

	private final Plugin plugin;

	public PaperScheduler(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public void runForPlayer(Player player, Runnable runnable) {
		Bukkit.getScheduler().runTask(this.plugin, runnable);
	}

	@Override
	public void runAsyncNow(Runnable runnable) {
		Bukkit.getScheduler().runTaskAsynchronously(this.plugin, runnable);
	}

	@Override
	public void runAsyncAtFixedRate(Runnable runnable, long delay, long period) {
		Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, runnable, delay, period);
	}

	@Override
	public void cancelTasks() {
		Bukkit.getScheduler().cancelTasks(this.plugin);
	}
}
