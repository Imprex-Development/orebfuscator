package net.imprex.orebfuscator.compatibility.paper;

import org.bukkit.plugin.Plugin;

import dev.imprex.orebfuscator.config.api.Config;
import net.imprex.orebfuscator.compatibility.CompatibilityScheduler;
import net.imprex.orebfuscator.compatibility.bukkit.BukkitScheduler;

public class PaperCompatibilityLayer extends AbstractPaperCompatibilityLayer {

	private final Thread mainThread = Thread.currentThread();

	private final BukkitScheduler scheduler;

	public PaperCompatibilityLayer(Plugin plugin, Config config) {
		this.scheduler = new BukkitScheduler(plugin);
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
