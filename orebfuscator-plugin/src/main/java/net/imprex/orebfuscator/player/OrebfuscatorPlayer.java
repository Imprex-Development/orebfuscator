package net.imprex.orebfuscator.player;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.google.common.base.Objects;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.config.AdvancedConfig;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkPosition;

public class OrebfuscatorPlayer {

	private final Player player;
	private final AdvancedConfig config;

	private final AtomicReference<World> world = new AtomicReference<>();
	private final Map<Long, Set<BlockPos>> chunks = new ConcurrentHashMap<>();

	private volatile long latestUpdateTimestamp = System.currentTimeMillis();
	private volatile Location location = new Location(null, 0, 0, 0);

	public OrebfuscatorPlayer(Orebfuscator orebfuscator, Player player) {
		this.player = player;
		this.config = orebfuscator.getOrebfuscatorConfig().advanced();
		this.location = player.getLocation();
	}

	/**
	 * Returns true if the last proximity update is longer ago then the configured
	 * proximity player interval (default 5s) or if the players location since the
	 * last update change according to the given rotation boolean and the
	 * {@link OrebfuscatorPlayer#isLocationSimilar isLocationSimilar} method.
	 * 
	 * @param rotation passed to the <code>isLocationSimilar</code> method
	 * @return true if a proximity update is needed
	 */
	public boolean needsProximityUpdate(boolean rotation) {
		if (!this.player.isOnline()) {
			return false;
		}

		long timestamp = System.currentTimeMillis();
		if (this.config.hasProximityPlayerCheckInterval() &&
				timestamp - this.latestUpdateTimestamp > this.config.proximityPlayerCheckInterval()) {

			// always update location + latestUpdateTimestamp on update
			this.location = location;
			this.latestUpdateTimestamp = timestamp;

			return true;
		}


		Location location = this.player.getLocation();
		if (isLocationSimilar(rotation, this.location, location)) {
			return false;
		}

		// always update location + latestUpdateTimestamp on update
		this.location = location;
		this.latestUpdateTimestamp = timestamp;
		
		return true;
	}

	/**
	 * Returns true if the worlds are the same and the distance between the locations
	 * is less then 0.5. If the rotation boolean is set this method also check if the
	 * yaw changed less then 10deg and the pitch less then 5deg.
	 * 
	 * @param rotation should rotation be checked
	 * @param a the first location
	 * @param b the second location
	 * @return if the locations are similar
	 */
	private static boolean isLocationSimilar(boolean rotation, Location a, Location b) {
		// check if world changed
		if (!Objects.equal(a.getWorld(), b.getWorld())) {
			return false;
		}

		// check if len(xyz) changed less then 0.5 blocks
		if (a.distanceSquared(b) > 0.25) {
			return false;
		}

		// check if rotation changed less then 5deg yaw or 2.5deg pitch
		if (rotation && (Math.abs(a.getYaw() - b.getYaw()) > 5 || Math.abs(a.getPitch() - b.getPitch()) > 2.5)) {
			return false;
		}

		return true;
	}

	void updateWorld() {
		if (!this.player.isOnline()) {
			return;
		}
		
		World world = this.player.getWorld();
		if (!Objects.equal(this.world.getAndSet(world), world)) {
			this.chunks.clear();
		}
	}

	public void addChunk(int chunkX, int chunkZ, Set<BlockPos> blocks) {
		long key = ChunkPosition.toLong(chunkX, chunkZ);
		this.chunks.computeIfAbsent(key, k -> {
			return Collections.newSetFromMap(new ConcurrentHashMap<>());
		}).addAll(blocks);
	}

	public Set<BlockPos> getChunk(int chunkX, int chunkZ) {
		long key = ChunkPosition.toLong(chunkX, chunkZ);
		return this.chunks.getOrDefault(key, Collections.emptySet());
	}

	public void removeChunk(int chunkX, int chunkZ) {
		long key = ChunkPosition.toLong(chunkX, chunkZ);
		this.chunks.remove(key);
	}
}
