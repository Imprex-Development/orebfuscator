package net.imprex.orebfuscator.compatibility.folia;

import org.bukkit.plugin.Plugin;

import dev.imprex.orebfuscator.config.api.Config;
import net.imprex.orebfuscator.compatibility.CompatibilityScheduler;
import net.imprex.orebfuscator.compatibility.paper.AbstractPaperCompatibilityLayer;

public class FoliaCompatibilityLayer extends AbstractPaperCompatibilityLayer {

	private static final Class<?> TICK_THREAD_CLASS = getTickThreadClass();

	private static Class<?> getTickThreadClass() {
		try {
			return Class.forName("io.papermc.paper.threadedregions.TickRegionScheduler$TickThreadRunner");
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Can't find tick thread class for folia", e);
		}
	}

	private final FoliaScheduler scheduler;

	public FoliaCompatibilityLayer(Plugin plugin, Config config) {
		this.scheduler = new FoliaScheduler(plugin);
	}

	@Override
	public boolean isGameThread() {
		return TICK_THREAD_CLASS.isInstance(Thread.currentThread());
	}

	@Override
	public CompatibilityScheduler getScheduler() {
		return this.scheduler;
	}
}
