package net.imprex.orebfuscator.util;

public class BlockCoords {

	private static final int bitsPerX = 26;
	private static final int bitsPerZ = bitsPerX;
	private static final int bitsPerY = 64 - bitsPerX - bitsPerZ;

	private static final int offsetY = 0;
	private static final int offsetZ = offsetY + bitsPerY;
	private static final int offsetX = offsetZ + bitsPerZ;

	private static final long maskX = (1L << bitsPerX) - 1L;
	private static final long maskY = (1L << bitsPerY) - 1L;
	private static final long maskZ = (1L << bitsPerZ) - 1L;

	public final int x;
	public final int y;
	public final int z;

	public BlockCoords(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public long toLong() {
		return (this.x & maskX) << offsetX | (this.y & maskY) << offsetY | (this.z & maskZ) << offsetZ;
	}

	public static BlockCoords fromLong(long value) {
		int x = (int) (value << (64 - bitsPerX - offsetX) >> (64 - bitsPerX));
		int y = (int) (value << (64 - bitsPerY - offsetY) >> (64 - bitsPerY));
		int z = (int) (value << (64 - bitsPerZ - offsetZ) >> (64 - bitsPerZ));
		return new BlockCoords(x, y, z);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof BlockCoords)) {
			return false;
		} else {
			BlockCoords other = (BlockCoords) obj;
			return this.x == other.x && this.y == other.y && this.z == other.z;
		}
	}

	@Override
	public int hashCode() {
		return this.x ^ this.y ^ this.z;
	}

	@Override
	public String toString() {
		return "[" + this.x + ", " + this.y + ", " + this.z + "]";
	}
}
