package net.imprex.orebfuscator.player;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.imprex.orebfuscator.Orebfuscator;

public class OrebfuscatorPlayerMap implements Listener {

	private final Orebfuscator orebfuscator;

	private final ConcurrentMap<Player, OrebfuscatorPlayer> internalMap = new ConcurrentHashMap<>();

	public OrebfuscatorPlayerMap(Orebfuscator orebfuscator) {
		this.orebfuscator = orebfuscator;
		if (orebfuscator.getOrebfuscatorConfig().proximityEnabled()) {
			Bukkit.getPluginManager().registerEvents(this, orebfuscator);

			for (Player player : Bukkit.getOnlinePlayers()) {
				this.addPlayer(player);
			}
		}
	}

	private void addPlayer(Player player) {
		this.internalMap.put(player, new OrebfuscatorPlayer(orebfuscator, player));
	}

	@EventHandler
	public void onLogin(PlayerLoginEvent event) {
		this.addPlayer(event.getPlayer());
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		this.internalMap.remove(event.getPlayer());
	}

	public OrebfuscatorPlayer get(Player player) {
		OrebfuscatorPlayer orebfuscatorPlayer = this.internalMap.get(player);
		if (orebfuscatorPlayer != null) {
			orebfuscatorPlayer.updateWorld();
		}
		return orebfuscatorPlayer;
	}
}
