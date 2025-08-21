package net.imprex.orebfuscator.injector;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.imprex.orebfuscator.Orebfuscator;

public class OrebfuscatorInjectorManager implements Listener {

	private final Orebfuscator orebfuscator;
	private final Map<Player, OrebfuscatorInjector> injectors = new HashMap<>();

	public OrebfuscatorInjectorManager(Orebfuscator orebfuscator) {
		this.orebfuscator = orebfuscator;

		Bukkit.getPluginManager().registerEvents(this, orebfuscator);

		for (Player player : Bukkit.getOnlinePlayers()) {
			this.injectors.put(player, new OrebfuscatorInjector(this.orebfuscator, player));
		}

		this.orebfuscator.getStatistics().setAveragePacketDelay(() -> {
			return this.injectors.values().stream()
					.mapToDouble(OrebfuscatorInjector::averagePacketDelay)
					.average()
					.orElse(0d);
		});
	}

	@EventHandler(priority = EventPriority.LOW)
	public void handleJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		this.injectors.put(player, new OrebfuscatorInjector(this.orebfuscator, player));
	}

	@EventHandler(priority = EventPriority.LOW)
	public void handleQuit(PlayerQuitEvent event) {
		this.injectors.remove(event.getPlayer()).uninject();
	}

	public void close() {
		for (OrebfuscatorInjector injector : this.injectors.values()) {
			injector.uninject();
		}

		this.injectors.clear();
	}
}
