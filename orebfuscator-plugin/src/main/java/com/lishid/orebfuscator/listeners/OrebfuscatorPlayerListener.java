/*
 * Copyright (C) 2011-2014 lishid.  All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation,  version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.lishid.orebfuscator.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.lishid.orebfuscator.Orebfuscator;
import com.lishid.orebfuscator.hithack.BlockHitManager;
import com.lishid.orebfuscator.obfuscation.BlockUpdate;
import com.lishid.orebfuscator.obfuscation.ProximityHider;
import com.lishid.orebfuscator.utils.CommandSenderUtil;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.util.PermissionUtil;

public class OrebfuscatorPlayerListener implements Listener {

	private final OrebfuscatorConfig config;

	public OrebfuscatorPlayerListener(Orebfuscator orebfuscator) {
		this.config = orebfuscator.getOrebfuscatorConfig();
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if (this.config.general().bypassNotification() && PermissionUtil.canDeobfuscate(player)) {
			CommandSenderUtil.sendMessage(player, "Orebfuscator bypassed.");
		}

		if (this.config.proximityEnabled()) {
			ProximityHider.addPlayerToCheck(event.getPlayer(), null);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent event) {
		BlockHitManager.clearHistory(event.getPlayer());
		if (this.config.proximityEnabled()) {
			ProximityHider.clearPlayer(event.getPlayer());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.useInteractedBlock() == Result.DENY) {
			return;
		}

		// For using a hoe for farming
		if (event.getItem() != null && event.getItem().getType() != null
				&& (event.getMaterial() == Material.DIRT || event.getMaterial() == Material.GRASS)
				&& NmsInstance.get().isHoe(event.getItem().getType())) {
			BlockUpdate.update(event.getClickedBlock());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerChangeWorld(PlayerChangedWorldEvent event) {
		BlockHitManager.clearHistory(event.getPlayer());

		if (this.config.proximityEnabled()) {
			ProximityHider.clearBlocksForOldWorld(event.getPlayer());
			ProximityHider.addPlayerToCheck(event.getPlayer(), null);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTeleport(PlayerTeleportEvent event) {
		if (this.config.proximityEnabled()) {
			if (event.getCause() != TeleportCause.END_PORTAL && event.getCause() != TeleportCause.NETHER_PORTAL) {
				ProximityHider.addPlayerToCheck(event.getPlayer(), null);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerMove(PlayerMoveEvent event) {
		if (this.config.proximityEnabled()) {
			ProximityHider.addPlayerToCheck(event.getPlayer(), event.getFrom());
		}
	}
}
