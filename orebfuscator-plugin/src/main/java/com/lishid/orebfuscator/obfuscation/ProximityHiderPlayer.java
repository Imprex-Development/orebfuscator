/**
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.obfuscation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.World;

import net.imprex.orebfuscator.util.BlockCoords;

public class ProximityHiderPlayer {
	private World world;
	private Map<Long, List<BlockCoords>> chunks;

	public ProximityHiderPlayer(World world) {
		this.world = world;
		this.chunks = new HashMap<>();
	}

	public World getWorld() {
		return this.world;
	}

	public void setWorld(World world) {
		this.world = world;
	}

	public void clearChunks() {
		this.chunks.clear();
	}

	public void putBlocks(int chunkX, int chunkZ, List<BlockCoords> blocks) {
		long key = getKey(chunkX, chunkZ);
		this.chunks.put(key, blocks);
	}

	public List<BlockCoords> getBlocks(int chunkX, int chunkZ) {
		long key = getKey(chunkX, chunkZ);
		return this.chunks.get(key);
	}

	public void copyChunks(ProximityHiderPlayer playerInfo) {
		this.chunks.putAll(playerInfo.chunks);
	}

	public void removeChunk(int chunkX, int chunkZ) {
		long key = getKey(chunkX, chunkZ);
		this.chunks.remove(key);
	}

	private static long getKey(int chunkX, int chunkZ) {
		return (chunkZ & 0xffffffffL) << 32 | chunkX & 0xffffffffL;
	}
}
