package net.imprex.orebfuscator.cache;

import java.util.ArrayList;
import java.util.List;

import net.imprex.orebfuscator.util.BlockCoords;
import net.imprex.orebfuscator.util.EngineMode;

public class ChunkCacheEntry {

	private final long hash;
	private final EngineMode engineMode;

	private final byte[] data;

	private final List<BlockCoords> proximityBlocks = new ArrayList<>();
	private final List<BlockCoords> removedEntities = new ArrayList<>();

	public ChunkCacheEntry(long hash, EngineMode engineMode, byte[] data) {
		this.hash = hash;
		this.engineMode = engineMode;
		this.data = data;
	}

	public long getHash() {
		return hash;
	}

	public EngineMode getEngineMode() {
		return engineMode;
	}

	public byte[] getData() {
		return data;
	}

	public List<BlockCoords> getProximityBlocks() {
		return proximityBlocks;
	}

	public List<BlockCoords> getRemovedEntities() {
		return removedEntities;
	}
}
