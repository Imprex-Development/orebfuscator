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

import java.util.HashMap;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.lishid.orebfuscator.Orebfuscator;

public class BlockHitManager {
	
	private static final int ANTI_HIT_HACK_DECREMENT_FACTOR = 1000;
	private static final int ANIT_HIT_HACK_MAX_VIOLATION = 15;

	private static HashMap<Player, PlayerBlockTracking> playersBlockTrackingStatus = new HashMap<>();
	private static Orebfuscator orebfuscator;

	public static void initialize(Orebfuscator orebfuscator) {
		BlockHitManager.orebfuscator = orebfuscator;
	}

	public static boolean hitBlock(Player player, Block block) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			return true;
		}

		PlayerBlockTracking playerBlockTracking = getPlayerBlockTracking(player);

		if (playerBlockTracking.isBlock(block)) {
			return true;
		}

		long time = playerBlockTracking.getTimeDifference();
		playerBlockTracking.incrementHackingIndicator();
		playerBlockTracking.setBlock(block);
		playerBlockTracking.updateTime();

		int decrement = (int) (time / ANTI_HIT_HACK_DECREMENT_FACTOR);
		playerBlockTracking.decrementHackingIndicator(decrement);

		if (playerBlockTracking.getHackingIndicator() == ANIT_HIT_HACK_MAX_VIOLATION) {
			playerBlockTracking.incrementHackingIndicator(ANIT_HIT_HACK_MAX_VIOLATION);
		}

		if (playerBlockTracking.getHackingIndicator() > ANIT_HIT_HACK_MAX_VIOLATION) {
			return false;
		}

		return true;
	}

	public static boolean canFakeHit(Player player) {
		PlayerBlockTracking playerBlockTracking = getPlayerBlockTracking(player);

		if (playerBlockTracking.getHackingIndicator() > ANIT_HIT_HACK_MAX_VIOLATION) {
			return false;
		}

		return true;
	}

	public static boolean fakeHit(Player player) {
		PlayerBlockTracking playerBlockTracking = getPlayerBlockTracking(player);
		playerBlockTracking.incrementHackingIndicator();

		if (playerBlockTracking.getHackingIndicator() > ANIT_HIT_HACK_MAX_VIOLATION) {
			return false;
		}

		return true;
	}

	public static void breakBlock(Player player, Block block) {
		if (player.getGameMode() == GameMode.CREATIVE) {
			return;
		}

		PlayerBlockTracking playerBlockTracking = getPlayerBlockTracking(player);
		if (playerBlockTracking.isBlock(block)) {
			playerBlockTracking.decrementHackingIndicator(2);
		}
	}

	private static PlayerBlockTracking getPlayerBlockTracking(Player player) {
		if (!playersBlockTrackingStatus.containsKey(player)) {
			playersBlockTrackingStatus.put(player, new PlayerBlockTracking(BlockHitManager.orebfuscator, player));
		}
		return playersBlockTrackingStatus.get(player);
	}

	public static void clearHistory(Player player) {
		playersBlockTrackingStatus.remove(player);
	}

	public static void clearAll() {
		playersBlockTrackingStatus.clear();
	}
}