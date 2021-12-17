package net.imprex.orebfuscator;

import java.util.HashMap;
import java.util.Map;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;

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
		this.metrics.addCustomChart(new DrilldownPie("systemMemory", () -> {
			final Map<String, Map<String, Integer>> result = new HashMap<>();
			final Map<String, Integer> exact = new HashMap<>();

			long memory = Runtime.getRuntime().maxMemory();
			if (memory == Long.MAX_VALUE) {
				result.put("unlimited", exact);
			} else {
				float gibiByte = Math.round(memory / 1073741824f * 100f) / 100f;
				exact.put(gibiByte + "GiB", 1);
				result.put(MathUtil.ceilToPowerOfTwo((int) gibiByte) + "GiB", exact);
			}

			return result;
		}));
	}

	public void addFastGazeChart(OrebfuscatorConfig config) {
		this.metrics.addCustomChart(new SimplePie("fast_gaze", () -> {
			return Boolean.toString(config.usesFastGaze());
		}));
	}
}
