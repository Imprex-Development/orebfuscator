package net.imprex.orebfuscator.compatibility.folia;

import org.bukkit.plugin.Plugin;

import net.imprex.orebfuscator.compatibility.CompatibilityScheduler;
import net.imprex.orebfuscator.compatibility.paper.AbstractPaperCompatibilityLayer;
import net.imprex.orebfuscator.config.Config;

public class FoliaCompatibilityLayer extends AbstractPaperCompatibilityLayer {

	private final FoliaScheduler scheduler;

	public FoliaCompatibilityLayer(Plugin plugin, Config config) {
		this.scheduler = new FoliaScheduler(plugin);
	}

	@Override
	public CompatibilityScheduler getScheduler() {
		return this.scheduler;
	}
}
