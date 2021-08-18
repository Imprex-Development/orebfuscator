package net.imprex.orebfuscator;

import org.bstats.bukkit.Metrics;

import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.util.MathUtil;

public class MetricsSystem {

	private final Metrics metrics;

	public MetricsSystem(Orebfuscator orebfuscator) {
		this.metrics = new Metrics(orebfuscator, 8942);
		this.addMemoryChart();
		this.addFastGazeChart(orebfuscator.getOrebfuscatorConfig());
	}

	public void addMemoryChart() {
		this.metrics.addCustomChart(new Metrics.SimplePie("systemMemory", () -> {
			int memory = (int) (Runtime.getRuntime().maxMemory() / 1073741824L);
			return MathUtil.ceilToPowerOfTwo(memory) + "GiB";
		}));
	}

	public void addFastGazeChart(OrebfuscatorConfig config) {
		this.metrics.addCustomChart(new Metrics.SimplePie("fast_gaze", () -> {
			return Boolean.toString(config.usesFastGaze());
		}));
	}
}
