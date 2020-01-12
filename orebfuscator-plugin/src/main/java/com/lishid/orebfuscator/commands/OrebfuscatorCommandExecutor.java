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

package com.lishid.orebfuscator.commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.lishid.orebfuscator.Orebfuscator;
import com.lishid.orebfuscator.config.ConfigManager;
import com.lishid.orebfuscator.config.WorldConfig;
import com.lishid.orebfuscator.utils.CommandSenderUtil;
import com.lishid.orebfuscator.utils.Globals;
import com.lishid.orebfuscator.utils.MaterialHelper;

import net.imprex.orebfuscator.NmsInstance;

public class OrebfuscatorCommandExecutor implements CommandExecutor {

	private final Orebfuscator orebfuscator;
	private final ConfigManager configManager;

	public OrebfuscatorCommandExecutor(Orebfuscator orebfuscator) {
		this.orebfuscator = orebfuscator;
		this.configManager = orebfuscator.getConfigManager();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (sender instanceof Player && !sender.hasPermission("orebfuscator.admin")) {
			CommandSenderUtil.sendMessage(sender, "You do not have permissions.");
			return true;
		}

		if (args.length <= 0) {
			return false;
		}

		if (args[0].equalsIgnoreCase("engine") && args.length > 1) {
			int engine = this.configManager.getConfig().getEngineMode();
			try {
				engine = new Integer(args[1]);
			} catch (NumberFormatException e) {
				CommandSenderUtil.sendMessage(sender, args[1] + " is not a number!");
				return true;
			}
			if (engine != 1 && engine != 2) {
				CommandSenderUtil.sendMessage(sender, args[1] + " is not a valid EngineMode!");
				return true;
			} else {
				this.configManager.setEngineMode(engine);
				CommandSenderUtil.sendMessage(sender, "Engine set to: " + engine);
				return true;
			}
		}

		else if (args[0].equalsIgnoreCase("updateradius") && args.length > 1) {
			int radius = this.configManager.getConfig().getUpdateRadius();
			try {
				radius = new Integer(args[1]);
			} catch (NumberFormatException e) {
				CommandSenderUtil.sendMessage(sender, args[1] + " is not a number!");
				return true;
			}
			this.configManager.setUpdateRadius(radius);
			CommandSenderUtil.sendMessage(sender, "UpdateRadius set to: " + this.configManager.getConfig().getUpdateRadius());
			return true;
		}

		else if (args[0].equalsIgnoreCase("initialradius") && args.length > 1) {
			int radius = this.configManager.getConfig().getInitialRadius();
			try {
				radius = new Integer(args[1]);
			} catch (NumberFormatException e) {
				CommandSenderUtil.sendMessage(sender, args[1] + " is not a number!");
				return true;
			}
			this.configManager.setInitialRadius(radius);
			CommandSenderUtil.sendMessage(sender, "InitialRadius set to: " + radius);
			return true;
		}

		else if (args[0].equalsIgnoreCase("enable") || args[0].equalsIgnoreCase("disable")) {
			boolean data = args[0].equalsIgnoreCase("enable");

			if (args[0].equalsIgnoreCase("enable") && args.length == 1) {
				this.configManager.setEnabled(true);
				CommandSenderUtil.sendMessage(sender, "Enabled.");
				return true;
			}

			else if (args[0].equalsIgnoreCase("disable") && args.length == 1) {
				this.configManager.setEnabled(false);
				CommandSenderUtil.sendMessage(sender, "Disabled.");
				return true;
			}

			else if (args.length > 1) {
				if (args[1].equalsIgnoreCase("op")) {
					this.configManager.setNoObfuscationForOps(data);
					CommandSenderUtil.sendMessage(sender, "Ops No-Obfuscation " + (data ? "enabled" : "disabled") + ".");
					return true;
				} else if (args[1].equalsIgnoreCase("perms") || args[1].equalsIgnoreCase("permissions")) {
					this.configManager.setNoObfuscationForPermission(data);
					CommandSenderUtil.sendMessage(sender, "Permissions No-Obfuscation " + (data ? "enabled" : "disabled") + ".");
					return true;
				} else if (args[1].equalsIgnoreCase("cache")) {
					this.configManager.setUseCache(data);
					CommandSenderUtil.sendMessage(sender, "Cache " + (data ? "enabled" : "disabled") + ".");
					return true;
				} else if (args[1].equalsIgnoreCase("notification")) {
					this.configManager.setLoginNotification(data);
					CommandSenderUtil.sendMessage(sender, "Login Notification " + (data ? "enabled" : "disabled") + ".");
					return true;
				}
			}
		}

		else if (args[0].equalsIgnoreCase("reload")) {
			this.configManager.reload();
			CommandSenderUtil.sendMessage(sender, "Reload complete.");
			return true;
		}

		else if (args[0].equalsIgnoreCase("status")) {
			CommandSenderUtil.sendMessage(sender,
					"Orebfuscator " + this.orebfuscator.getDescription().getVersion());
			CommandSenderUtil.sendMessage(sender, "Engine Mode: " + this.configManager.getConfig().getEngineMode());

			CommandSenderUtil.sendMessage(sender, "Caching: " + (this.configManager.getConfig().isUseCache() ? "Enabled" : "Disabled"));
			CommandSenderUtil.sendMessage(sender,
					"ProximityHider: " + (this.configManager.getConfig().isProximityHiderEnabled() ? "Enabled" : "Disabled"));

			CommandSenderUtil.sendMessage(sender, "Initial Obfuscation Radius: " + this.configManager.getConfig().getInitialRadius());
			CommandSenderUtil.sendMessage(sender, "Update Radius: " + this.configManager.getConfig().getUpdateRadius());

			String worldNames = this.configManager.getConfig().getWorldNames();

			CommandSenderUtil.sendMessage(sender, "Worlds in List: " + (worldNames.equals("") ? "None" : worldNames));
			return true;
		}

		else if (args[0].equalsIgnoreCase("clearcache")) {
			// TODO delete file and memory cache
			// ObfuscatedDataCache.clearCache();
			CommandSenderUtil.sendMessage(sender, "Cache cleared.");

			return true;
		}

		else if (args[0].equalsIgnoreCase("obfuscateblocks")) {
			commandObfuscateBlocks(sender, args);
			return true;
		}

		else if (args[0].equalsIgnoreCase("ph")) {
			commandProximityHider(sender, args);
			return true;
		}

		else if (args[0].equalsIgnoreCase("lm")) {
			commandListMaterials(sender, args);
			return true;
		}

		else if (args[0].equalsIgnoreCase("tp")) {
			commandTransparentBlocks(sender, args);
			return true;
		}

		return false;
	}

	private void commandObfuscateBlocks(CommandSender sender, String[] args) {
		if (args.length == 1) {
			CommandSenderUtil.sendMessage(sender, ChatColor.RED + "World is required parameter.");
			return;
		}

		String worldName = args[1];
		World world = Bukkit.getWorld(worldName);

		if (world == null) {
			CommandSenderUtil.sendMessage(sender, ChatColor.RED + "Specified world is not found.");
			return;
		}

		if (args.length > 2) {
			Material material = Material.getMaterial(args[2]);

			if (material == null) {
				CommandSenderUtil.sendMessage(sender, ChatColor.RED + "Specified material is not found.");
			} else {
				int materialId = NmsInstance.get().getMaterialIds(material).iterator().next();

				if ((this.configManager.getWorld(world).getObfuscatedBits(materialId)
						& Globals.MASK_OBFUSCATE) != 0) {
					CommandSenderUtil.sendMessage(sender, material.name() + ": " + ChatColor.GREEN + "obfuscate");
				} else {
					CommandSenderUtil.sendMessage(sender, material.name() + ": " + ChatColor.RED + "not obfuscate");
				}
			}

			return;
		}

		Material[] materials = Material.values();
		ArrayList<String> blockNames = new ArrayList<>();

		for (Material material : materials) {
			if (material.isBlock()) {
				int blockId = NmsInstance.get().getMaterialIds(material).iterator().next();
				int bits = this.configManager.getWorld(world).getObfuscatedBits(blockId);

				if (bits != 0) {
					blockNames.add(material.name() + " " + ChatColor.WHITE + bits);
				}
			}
		}

		Collections.sort(blockNames);

		StringBuilder blocks = new StringBuilder();
		blocks.append("Obfuscate blocks:");

		if (blockNames.size() > 0) {
			for (String blockName : blockNames) {
				blocks.append(ChatColor.GREEN + "\n - " + blockName);
			}
		} else {
			blocks.append(" None");
		}

		CommandSenderUtil.sendMessage(sender, blocks.toString());
	}

	private void commandProximityHider(CommandSender sender, String[] args) {
		if (args.length == 1) {
			CommandSenderUtil.sendMessage(sender, ChatColor.RED + "World is required parameter.");
			return;
		}

		WorldConfig worldConfig = null;
		String worldName = args[1];

		if (worldName.startsWith(":")) {
//			if (worldName.equalsIgnoreCase(":default")) {
//				worldConfig = Orebfuscator.config.getDefaultWorld();
//			} else if (worldName.equalsIgnoreCase(":normal")) {
//				worldConfig = Orebfuscator.config.getNormalWorld();
//			} else if (worldName.equalsIgnoreCase(":nether")) {
//				worldConfig = Orebfuscator.config.getNetherWorld();
//			} else if (worldName.equalsIgnoreCase(":end")) {
//				worldConfig = Orebfuscator.config.getEndWorld();
//			}
		} else {
			World world = Bukkit.getWorld(worldName);
			worldConfig = this.configManager.getWorld(world);
		}

		if (worldConfig == null) {
			CommandSenderUtil.sendMessage(sender, ChatColor.RED + "Specified world is not found.");
			return;
		}

		CommandSenderUtil.sendMessage(sender,
				"ProximityHider: " + (worldConfig.getProximityHiderConfig().isEnabled() ? "Enabled" : "Disabled"));

		StringBuilder blocks = new StringBuilder();
		blocks.append("Obfuscate blocks:");

		Set<Integer> blockIds = worldConfig.getProximityHiderConfig().getProximityHiderBlocks();

		if (blockIds.size() > 0) {
			ArrayList<String> blockNames = new ArrayList<>();

			for (int id : blockIds) {
				blockNames.add(MaterialHelper.getById(id).name());
			}

			Collections.sort(blockNames);

			for (String blockName : blockNames) {
				blocks.append("\n - " + blockName);
			}
		} else {
			blocks.append(" None");
		}

		CommandSenderUtil.sendMessage(sender, blocks.toString());
	}

	private void commandListMaterials(CommandSender sender, String[] args) {
		Material[] materials = Material.values();

		List<String> blockNames = new ArrayList<>();

		for (Material material : materials) {
			if (material.isBlock()) {
				List<Integer> ids = new ArrayList<>(NmsInstance.get().getMaterialIds(material));
				Collections.sort(ids);

				for (int id : ids) {
					blockNames.add(material.name() + " = " + id);
				}
			}
		}

		Collections.sort(blockNames);

		StringBuilder blocks = new StringBuilder();

		for (String blockName : blockNames) {
			blocks.append("\n - " + blockName);
		}

		CommandSenderUtil.sendMessage(sender, blocks.toString());
	}

	private void commandTransparentBlocks(CommandSender sender, String[] args) {
		Material[] materials = Material.values();

		List<String> transparentBlockNames = new ArrayList<>();
		List<String> nonTransparentBlockNames = new ArrayList<>();

		for (Material material : materials) {
			if (material.isBlock()) {
				int blockId = NmsInstance.get().getMaterialIds(material).iterator().next();
				boolean isTransparent = this.configManager.getConfig().isBlockTransparent(blockId);

				if (isTransparent) {
					transparentBlockNames.add(material.name());
				} else {
					nonTransparentBlockNames.add(material.name());
				}
			}
		}

		Collections.sort(transparentBlockNames);
		Collections.sort(nonTransparentBlockNames);

		StringBuilder blocks = new StringBuilder();
		blocks.append("Transparent blocks:");

		for (String blockName : transparentBlockNames) {
			blocks.append("\n - " + blockName);
		}

		blocks.append("\nNon-Transparent blocks:");

		for (String blockName : nonTransparentBlockNames) {
			blocks.append("\n - " + blockName);
		}

		CommandSenderUtil.sendMessage(sender, blocks.toString());
	}
}