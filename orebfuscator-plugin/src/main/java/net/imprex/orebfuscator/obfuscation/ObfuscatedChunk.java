package net.imprex.orebfuscator.obfuscation;

import java.util.HashSet;
import java.util.Set;

import net.imprex.orebfuscator.util.BlockPos;

public class ObfuscatedChunk {

	private final byte[] hash;
	private final byte[] data;

	private final Set<BlockPos> proximityBlocks;
	private final Set<BlockPos> removedTileEntities;

	public ObfuscatedChunk(byte[] hash, byte[] data) {
		this(hash, data, new HashSet<>(), new HashSet<>());
	}

	public ObfuscatedChunk(byte[] hash, byte[] data, Set<BlockPos> proximityBlocks,
			Set<BlockPos> removedTileEntities) {
		this.hash = hash;
		this.data = data;
		this.proximityBlocks = proximityBlocks;
		this.removedTileEntities = removedTileEntities;
	}

	public byte[] getHash() {
		return this.hash;
	}

	public byte[] getData() {
		return this.data;
	}

	public Set<BlockPos> getProximityBlocks() {
		return this.proximityBlocks;
	}

	public Set<BlockPos> getRemovedTileEntities() {
		return this.removedTileEntities;
	}
}
