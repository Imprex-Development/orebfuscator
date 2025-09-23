package net.imprex.orebfuscator;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;

import dev.imprex.orebfuscator.config.OrebfuscatorConfig;
import dev.imprex.orebfuscator.util.MathUtil;

public class MetricsSystem {

	private static final NavigableMap<Integer, String> PLAYER_COUNT_GROUPS = new TreeMap<>();

	static {
		PLAYER_COUNT_GROUPS.put(8, "0-8");
		PLAYER_COUNT_GROUPS.put(16, "9-16");
		PLAYER_COUNT_GROUPS.put(32, "17-32");
		PLAYER_COUNT_GROUPS.put(64, "33-64");
		PLAYER_COUNT_GROUPS.put(128, "65-128");
		PLAYER_COUNT_GROUPS.put(Integer.MAX_VALUE, ">129");
	}

	private final Metrics metrics;

	public MetricsSystem(Orebfuscator orebfuscator) {
		this.metrics = new Metrics(orebfuscator, 8942);
		this.addMemoryChart();
		this.addPlayerCountChart();
		this.addConfigCharts(orebfuscator.config());
		this.addUsageCharts(orebfuscator.config());
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

	public void addPlayerCountChart() {
		this.metrics.addCustomChart(new SimplePie("player_count", () -> {
			int playerCount = Bukkit.getOnlinePlayers().size();
			return PLAYER_COUNT_GROUPS.ceilingEntry(playerCount).getValue();
		}));
	}

	public void addConfigCharts(OrebfuscatorConfig config) {
		this.metrics.addCustomChart(new SimplePie("max_mspt", () -> {
			return Integer.toString(config.advanced().maxMillisecondsPerTick());
		}));
		this.metrics.addCustomChart(new SimplePie("update_radius", () -> {
			return Integer.toString(config.general().updateRadius());
		}));
	}

	public void addUsageCharts(OrebfuscatorConfig config) {
		this.metrics.addCustomChart(new SimplePie("check_for_updates", () -> {
			return Boolean.toString(config.general().checkForUpdates());
		}));
		this.metrics.addCustomChart(new SimplePie("ignore_spectator", () -> {
			return Boolean.toString(config.general().ignoreSpectator());
		}));
		this.metrics.addCustomChart(new SimplePie("cache", () -> {
			return Boolean.toString(config.cache().enabled());
		}));
		this.metrics.addCustomChart(new SimplePie("proximity", () -> {
			return Boolean.toString(config.proximityEnabled());
		}));
		this.metrics.addCustomChart(new SimplePie("block_specific_config", () -> {
			return Boolean.toString(config.usesBlockSpecificConfigs());
		}));
		this.metrics.addCustomChart(new SimplePie("frustum_culling", () -> {
			return Boolean.toString(config.usesFrustumCulling());
		}));
		this.metrics.addCustomChart(new SimplePie("ray_cast", () -> {
			return config.usesRayCastCheck();
		}));
	}
}
