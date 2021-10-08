package net.imprex.orebfuscator.proximityhider;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import net.imprex.orebfuscator.Orebfuscator;

public class ProximityListener implements Listener {

	private ProximityHider proximityHider;

	public ProximityListener(Orebfuscator orebfuscator) {
		this.proximityHider = orebfuscator.getProximityHider();
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		this.proximityHider.queuePlayerUpdate(event.getPlayer());
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		this.proximityHider.removePlayer(event.getPlayer());
	}

	@EventHandler
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		this.proximityHider.queuePlayerUpdate(event.getPlayer());
	}

	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		// already called in PlayerChangedWorldEvent event
		if (!event.getFrom().getWorld().equals(event.getTo().getWorld())) {
			return;
		}

		this.proximityHider.queuePlayerUpdate(event.getPlayer());
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		this.proximityHider.queuePlayerUpdate(event.getPlayer());
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		this.proximityHider.queuePlayerUpdate(event.getPlayer());
	}
}