package net.imprex.orebfuscator.compatibility.folia;

import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import net.imprex.orebfuscator.compatibility.CompatibilityScheduler;

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
	public void runAsyncAtFixedRate(Runnable runnable, long delay, long period) {
		if (this.plugin.isEnabled()) {
			Bukkit.getAsyncScheduler().runAtFixedRate(this.plugin, task -> runnable.run(),
					delay * 50, period * 50, TimeUnit.MILLISECONDS);
		}
	}

	@Override
	public void cancelTasks() {
		Bukkit.getAsyncScheduler().cancelTasks(this.plugin);
	}
}
