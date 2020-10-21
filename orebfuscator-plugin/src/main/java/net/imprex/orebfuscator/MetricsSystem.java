package net.imprex.orebfuscator;

import java.util.HashMap;
import java.util.Map;

import org.bstats.bukkit.Metrics;

public class MetricsSystem {

	private final Metrics metrics;

	public MetricsSystem(Orebfuscator orebfuscator) {
		this.metrics = new Metrics(orebfuscator, 8942);
		this.addMemoryChart();
	}

	public void addMemoryChart() {
		this.metrics.addCustomChart(new Metrics.DrilldownPie("system_memory", () -> {
			final Map<String, Map<String, Integer>> result = new HashMap<>();
			final Map<String, Integer> exact = new HashMap<>();

			long memory = Runtime.getRuntime().maxMemory();
			if (memory == Long.MAX_VALUE) {
				exact.put("unbound", 1);
				result.put("unbound", exact);
			} else {
				long megaByte = (long) Math.ceil(memory / 1048576d);
				long gigaByte = (long) Math.ceil(memory / 1073741824d);
				exact.put(megaByte + "MiB", 1);
				result.put(gigaByte + "GiB", exact);
			}

			return result;
		}));
	}
}
