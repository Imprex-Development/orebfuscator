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

package com.lishid.orebfuscator;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.lishid.orebfuscator.cache.CacheCleaner;
import com.lishid.orebfuscator.chunkmap.ChunkMapBuffer;
import com.lishid.orebfuscator.commands.OrebfuscatorCommandExecutor;
import com.lishid.orebfuscator.config.ConfigManager;
import com.lishid.orebfuscator.config.OrebfuscatorConfig;
import com.lishid.orebfuscator.hithack.BlockHitManager;
import com.lishid.orebfuscator.hook.ProtocolLibHook;
import com.lishid.orebfuscator.listeners.OrebfuscatorBlockListener;
import com.lishid.orebfuscator.listeners.OrebfuscatorEntityListener;
import com.lishid.orebfuscator.listeners.OrebfuscatorPlayerListener;
import com.lishid.orebfuscator.obfuscation.BlockUpdate;
import com.lishid.orebfuscator.obfuscation.Calculations;
import com.lishid.orebfuscator.obfuscation.ProximityHider;
import com.lishid.orebfuscator.utils.Globals;
import com.lishid.orebfuscator.utils.MaterialHelper;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.cache.ChunkCache;

/**
 * Orebfuscator Anti X-RAY
 *
 * @author lishid
 */
public class Orebfuscator extends JavaPlugin implements Listener {

	public static final Logger LOGGER = Logger.getLogger("Minecraft.OFC");

	/**
	 * Log an information
	 */
	public static void log(String text) {
		LOGGER.info(Globals.LOG_PREFIX + text);
	}

	/**
	 * Log an error
	 */
	public static void log(Throwable e) {
		LOGGER.severe(Globals.LOG_PREFIX + e.toString());
		e.printStackTrace();
	}

	private ConfigManager configManager;
	private ChunkCache chunkCache;

	@Override
	public void onEnable() {
		try {
			PluginManager pluginManager = this.getServer().getPluginManager();

			// Check if protocolLib is enabled
			if (pluginManager.getPlugin("ProtocolLib") == null) {
				Orebfuscator.log("ProtocolLib is not found! Plugin cannot be enabled.");
				return;
			}

			// TODO will always throw nullpointerexc
			NmsInstance.initialize(this.configManager.getConfig());

			MaterialHelper.initialize();
			ChunkMapBuffer.initialize(NmsInstance.get().getBitsPerBlock());

			// Load configurations
			this.configManager = new ConfigManager(this);
			this.configManager.initialize();

			this.chunkCache = new ChunkCache(this);

			BlockHitManager.initialize(this);
			BlockUpdate.initialize(this);
			Calculations.initialize(this);
			ProximityHider.initialize(this);

			// Register events
			pluginManager.registerEvents(new OrebfuscatorPlayerListener(this), this);
			pluginManager.registerEvents(new OrebfuscatorEntityListener(), this);
			pluginManager.registerEvents(new OrebfuscatorBlockListener(this), this);

			// Register command
			this.getCommand("ofc").setExecutor(new OrebfuscatorCommandExecutor(this));

			// Register protocolLib
			new ProtocolLibHook(this).register();

			// Run CacheCleaner
			this.getServer().getScheduler().runTaskTimerAsynchronously(this, new CacheCleaner(this.configManager), 0,
					this.configManager.getConfig().getCacheCleanRate());
		} catch(Exception e) {
			e.printStackTrace();
			LOGGER.log(Level.SEVERE, "An error occurred by enabling plugin");

			this.getServer().getPluginManager().registerEvent(PluginEnableEvent.class, this, EventPriority.NORMAL, (listener, event) -> {
				PluginEnableEvent enableEvent = (PluginEnableEvent) event;

				if (enableEvent.getPlugin() == this) {
					HandlerList.unregisterAll(listener);
					Bukkit.getPluginManager().disablePlugin(this);
				}
			}, this);
		}
	}

	@Override
	public void onDisable() {
		this.chunkCache.invalidateAll();

		BlockHitManager.clearAll();

		this.getServer().getScheduler().cancelTasks(this);

		this.configManager = null;
	}

	public void runTask(Runnable task) {
		if (this.isEnabled()) {
			this.getServer().getScheduler().runTask(this, task);
		}
	}

	public ConfigManager getConfigManager() {
		return this.configManager;
	}

	public OrebfuscatorConfig getOrebfuscatorConfig() {
		return this.configManager.getConfig();
	}

	public ChunkCache getChunkCache() {
		return chunkCache;
	}
}
