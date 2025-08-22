package net.imprex.orebfuscator.obfuscation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.ChunkCacheKey;

public class ObfuscationResult {

	private final ChunkCacheKey key;

	private final byte[] hash;
	private final byte[] data;

	private final Set<BlockPos> blockEntities;
	private final List<BlockPos> proximityBlocks;

	public ObfuscationResult(ChunkCacheKey key, byte[] hash, byte[] data) {
		this(key, hash, data, new HashSet<>(), new ArrayList<>());
	}

	public ObfuscationResult(ChunkCacheKey key, byte[] hash, byte[] data,
			Set<BlockPos> blockEntities, List<BlockPos> proximityBlocks) {
		this.key = key;
		this.hash = hash;
		this.data = data;
		this.blockEntities = blockEntities;
		this.proximityBlocks = proximityBlocks;
	}

	public ChunkCacheKey getCacheKey() {
		return key;
	}

	public byte[] getHash() {
		return hash;
	}

	public byte[] getData() {
		return data;
	}

	public Set<BlockPos> getBlockEntities() {
		return blockEntities;
	}

	public List<BlockPos> getProximityBlocks() {
		return proximityBlocks;
	}
}
