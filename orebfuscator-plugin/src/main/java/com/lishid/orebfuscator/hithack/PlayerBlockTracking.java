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

package com.lishid.orebfuscator.hithack;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.util.OFCLogger;

public class PlayerBlockTracking {

	private final Orebfuscator orebfuscator;
	private final Player player;

	private Block block;
	private int hackingIndicator;
	private long lastTime = System.currentTimeMillis();

	public PlayerBlockTracking(Orebfuscator orebfuscator, Player player) {
		this.orebfuscator = orebfuscator;
		this.player = player;
	}

	public Player getPlayer() {
		return this.player;
	}

	public int getHackingIndicator() {
		return this.hackingIndicator;
	}

	public Block getBlock() {
		return this.block;
	}

	public boolean isBlock(Block block) {
		if (block == null || this.block == null) {
			return false;
		}
		return block.equals(this.block);
	}

	public void setBlock(Block block) {
		this.block = block;
	}

	public void incrementHackingIndicator(int value) {
		this.hackingIndicator += value;
		if (this.hackingIndicator >= 1 << 14) {
//			this.orebfuscator.runTask(new Runnable() {
//
//				@Override
//				public void run() {
//					String name = PlayerBlockTracking.this.player.getName();
//					OFCLogger.log("Player \"" + name + "\" tried to hack with packet spamming.");
//					PlayerBlockTracking.this.player.kickPlayer("End of Stream");
//				}
//			});
		}
	}

	public void incrementHackingIndicator() {
		this.incrementHackingIndicator(1);
	}

	public void decrementHackingIndicator(int value) {
		this.hackingIndicator -= value;
		if (this.hackingIndicator < 0) {
			this.hackingIndicator = 0;
		}
	}

	public void updateTime() {
		this.lastTime = System.currentTimeMillis();
	}

	public long getTimeDifference() {
		return System.currentTimeMillis() - this.lastTime;
	}
}
