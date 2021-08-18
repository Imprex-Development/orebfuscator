package net.imprex.orebfuscator;

import java.util.HashMap;
import java.util.Map;

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
		this.metrics.addCustomChart(new Metrics.DrilldownPie("systemMemory", () -> {
			final Map<String, Map<String, Integer>> result = new HashMap<>();
			final Map<String, Integer> exact = new HashMap<>();

			long memory = Runtime.getRuntime().maxMemory();
			if (memory == Long.MAX_VALUE) {
				result.put("unlimited", exact);
			} else {
				int gibiByte = (int) (memory / 1073741824L);
				exact.put(gibiByte + "GiB", 1);
				result.put(MathUtil.ceilToPowerOfTwo(gibiByte) + "GiB", exact);
			}

			return result;
		}));
	}

	public void addFastGazeChart(OrebfuscatorConfig config) {
		this.metrics.addCustomChart(new Metrics.SimplePie("fast_gaze", () -> {
			return Boolean.toString(config.usesFastGaze());
		}));
	}
}
