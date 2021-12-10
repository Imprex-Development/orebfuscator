package net.imprex.orebfuscator.obfuscation;

import java.util.HashSet;
import java.util.Set;

import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ObfuscationResult {

	private final ChunkPosition position;

	private final byte[] hash;
	private final byte[] data;

	private final Set<BlockPos> blockEntities;
	private final Set<BlockPos> proximityBlocks;

	public ObfuscationResult(ChunkPosition position, byte[] hash, byte[] data) {
		this(position, hash, data, new HashSet<>(), new HashSet<>());
	}

	public ObfuscationResult(ChunkPosition position, byte[] hash, byte[] data,
			Set<BlockPos> blockEntities, Set<BlockPos> proximityBlocks) {
		this.position = position;
		this.hash = hash;
		this.data = data;
		this.blockEntities = blockEntities;
		this.proximityBlocks = proximityBlocks;
	}

	public ChunkPosition getPosition() {
		return position;
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

	public Set<BlockPos> getProximityBlocks() {
		return proximityBlocks;
	}
}
