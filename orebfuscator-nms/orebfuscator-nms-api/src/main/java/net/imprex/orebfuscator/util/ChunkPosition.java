package net.imprex.orebfuscator.util;

import java.util.Objects;

public class ChunkPosition {

	private final String world;
	private final int x;
	private final int z;

	public ChunkPosition(String world, int x, int z) {
		this.world = Objects.requireNonNull(world);
		this.x = x;
		this.z = z;
	}

	public String getWorld() {
		return this.world;
	}

	public int getX() {
		return this.x;
	}

	public int getZ() {
		return this.z;
	}

	@Override
	public int hashCode() {
		int x = 1664525 * this.x + 1013904223;
		int z = 1664525 * (this.z ^ -559038737) + 1013904223;
		return this.world.hashCode() ^ x ^ z;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof ChunkPosition)) {
			return false;
		} else {
			ChunkPosition other = (ChunkPosition) obj;
			return this.x == other.x && this.z == other.z && Objects.equals(this.world, other.world);
		}
	}

	@Override
	public String toString() {
		return "[" + this.world + ", " + this.x + ", " + this.z + "]";
	}
}
