package net.imprex.orebfuscator.proximityhider;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.World;
import org.bukkit.entity.Player;

public class PlayerData {

	private final Lock lock = new ReentrantLock(true);
	private final Map<Player, Map<World, ProximityData>> playerData = new WeakHashMap<>();

	public void onPlayerJoin(Player player) {
		this.lock.lock();
		try {
			this.playerData.put(player, new HashMap<>());
		} finally {
			this.lock.unlock();
		}
	}

	public void onPlayerChangeWorld(Player player, World from) {
		this.lock.lock();
		try {
			Map<World, ProximityData> worldData = this.playerData.get(player);
			if (worldData != null) {
				worldData.remove(from);
			}
		} finally {
			this.lock.unlock();
		}
	}

	public void onPlayerQuit(Player player) {
		this.lock.lock();
		try {
			this.playerData.remove(player);
		} finally {
			this.lock.unlock();
		}
	}

	public ProximityData getDate(Player player, World world) {
		if (player != null && player.isOnline()) {
			this.lock.lock();
			try {
				Map<World, ProximityData> worldData = this.playerData.get(player);
				if (worldData != null) {
					return worldData.computeIfAbsent(world, ProximityData::new);
				}
			} finally {
				this.lock.unlock();
			}
		}
		return null;
	}
}
