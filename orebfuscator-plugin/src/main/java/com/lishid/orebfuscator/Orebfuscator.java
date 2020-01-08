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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.lishid.orebfuscator.cache.CacheCleaner;
import com.lishid.orebfuscator.cache.ObfuscatedDataCache;
import com.lishid.orebfuscator.chunkmap.ChunkMapBuffer;
import com.lishid.orebfuscator.commands.OrebfuscatorCommandExecutor;
import com.lishid.orebfuscator.config.ConfigManager;
import com.lishid.orebfuscator.config.OrebfuscatorConfig;
import com.lishid.orebfuscator.hithack.BlockHitManager;
import com.lishid.orebfuscator.hook.ProtocolLibHook;
import com.lishid.orebfuscator.listeners.OrebfuscatorBlockListener;
import com.lishid.orebfuscator.listeners.OrebfuscatorEntityListener;
import com.lishid.orebfuscator.listeners.OrebfuscatorPlayerListener;
import com.lishid.orebfuscator.nms.INmsManager;
import com.lishid.orebfuscator.utils.Globals;
import com.lishid.orebfuscator.utils.MaterialHelper;

/**
 * Orebfuscator Anti X-RAY
 *
 * @author lishid
 */
public class Orebfuscator extends JavaPlugin implements Listener {

	private static final String SERVER_VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
	private static final Pattern NMS_PATTERN = Pattern.compile("v(\\d+)_(\\d+)_R\\d", Pattern.DOTALL);

	public static final Logger LOGGER = Logger.getLogger("Minecraft.OFC");
	public static Orebfuscator instance;
	public static OrebfuscatorConfig config;
	public static ConfigManager configManager;

	private boolean isProtocolLibFound;

	public boolean getIsProtocolLibFound() {
		return this.isProtocolLibFound;
	}

	public Orebfuscator() {
		Orebfuscator.instance = this;
	}

	@Override
	public void onEnable() {
		try {
			NmsInstance.current = Orebfuscator.createNmsManager();

			MaterialHelper.init();
			ChunkMapBuffer.init(NmsInstance.current.getBitsPerBlock());

			// Load configurations
			this.loadOrebfuscatorConfig();

			PluginManager pm = this.getServer().getPluginManager();
			this.isProtocolLibFound = pm.getPlugin("ProtocolLib") != null;

			if (!this.isProtocolLibFound) {
				Orebfuscator.log("ProtocolLib is not found! Plugin cannot be enabled.");
				return;
			}

			// Orebfuscator events
			pm.registerEvents(new OrebfuscatorPlayerListener(), this);
			pm.registerEvents(new OrebfuscatorEntityListener(), this);
			pm.registerEvents(new OrebfuscatorBlockListener(), this);

			new ProtocolLibHook().register(this);

			// Run CacheCleaner
			this.getServer().getScheduler().runTaskTimerAsynchronously(this, new CacheCleaner(), 0,
					config.getCacheCleanRate());
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

	public void createConfigIfNotExist() {
		Path path = this.getDataFolder().toPath().resolve("config.yml");

		if (Files.notExists(path)) {
			try {
				Matcher matcher = Orebfuscator.NMS_PATTERN.matcher(Orebfuscator.SERVER_VERSION);

				if (!matcher.find()) {
					throw new RuntimeException("WTF is this version!?");
				}

				String configVersion = matcher.group(1) + "." + matcher.group(2);

				Files.copy(Orebfuscator.class.getResourceAsStream("/resources/config-" + configVersion + ".yml"), path);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void loadOrebfuscatorConfig() {
		this.createConfigIfNotExist();

		if (config == null) {
			config = new OrebfuscatorConfig();
			configManager = new ConfigManager(this, LOGGER, config);
		}

		configManager.load();

		ObfuscatedDataCache.resetCacheFolder();

		NmsInstance.current.setMaxLoadedCacheFiles(config.getMaxLoadedCacheFiles());

		// Make sure cache is cleared if config was changed since last start
		try {
			ObfuscatedDataCache.checkCacheAndConfigSynchronized();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void reloadOrebfuscatorConfig() {
		this.createConfigIfNotExist();
		this.reloadConfig();
		this.loadOrebfuscatorConfig();
	}

	private static INmsManager createNmsManager() {
		if (SERVER_VERSION.equals("v1_15_R1")) {
			return new net.imprex.orebfuscator.nms.v1_15_R1.NmsManager();
		} else if (SERVER_VERSION.equals("v1_14_R1")) {
			return new net.imprex.orebfuscator.nms.v1_14_R1.NmsManager();
		} else if (SERVER_VERSION.equals("v1_13_R2")) {
			return new com.lishid.orebfuscator.nms.v1_13_R2.NmsManager();
		} else if (SERVER_VERSION.equals("v1_13_R1")) {
			return new com.lishid.orebfuscator.nms.v1_13_R1.NmsManager();
		} else if (SERVER_VERSION.equals("v1_12_R1")) {
			return new com.lishid.orebfuscator.nms.v1_12_R1.NmsManager();
		} else if (SERVER_VERSION.equals("v1_11_R1")) {
			return new com.lishid.orebfuscator.nms.v1_11_R1.NmsManager();
		} else if (SERVER_VERSION.equals("v1_10_R1")) {
			return new com.lishid.orebfuscator.nms.v1_10_R1.NmsManager();
		} else if (SERVER_VERSION.equals("v1_9_R2")) {
			return new com.lishid.orebfuscator.nms.v1_9_R2.NmsManager();
		} else if (SERVER_VERSION.equals("v1_9_R1")) {
			return new com.lishid.orebfuscator.nms.v1_9_R1.NmsManager();
		} else if (SERVER_VERSION.equals("v1_8_R3")) {
			return new com.lishid.orebfuscator.nms.v1_8_R3.NmsManager();
		}

		return null;
	}

	@Override
	public void onDisable() {
		ObfuscatedDataCache.closeCacheFiles();
		BlockHitManager.clearAll();
		this.getServer().getScheduler().cancelTasks(this);
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		return OrebfuscatorCommandExecutor.onCommand(sender, command, label, args);
	}

	public void runTask(Runnable task) {
		if (this.isEnabled()) {
			this.getServer().getScheduler().runTask(this, task);
		}
	}

	/**
	 * Log an information
	 */
	public static void log(String text) {
		LOGGER.info(Globals.LogPrefix + text);
	}

	/**
	 * Log an error
	 */
	public static void log(Throwable e) {
		LOGGER.severe(Globals.LogPrefix + e.toString());
		e.printStackTrace();
	}

	/**
	 * Send a message to a player
	 */
	public static void message(CommandSender target, String message) {
		target.sendMessage(ChatColor.AQUA + Globals.LogPrefix + message);
	}
}
