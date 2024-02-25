package net.imprex.orebfuscator.util;

import org.bukkit.World;

public class BlockPos implements Comparable<BlockPos> {

	// from net.minecraft.core.BlockPos
	private static final int BITS_PER_X = 26;
	private static final int BITS_PER_Z = BITS_PER_X;
	private static final int BITS_PER_Y = 64 - BITS_PER_X - BITS_PER_Z;

	private static final int OFFSET_Y = 0;
	private static final int OFFSET_Z = OFFSET_Y + BITS_PER_Y;
	private static final int OFFSET_X = OFFSET_Z + BITS_PER_Z;

	private static final long MASK_X = (1L << BITS_PER_X) - 1L;
	private static final long MASK_Y = (1L << BITS_PER_Y) - 1L;
	private static final long MASK_Z = (1L << BITS_PER_Z) - 1L;
	
	// from net.minecraft.world.level.dimension.DimensionType
	public static final int Y_SIZE = (1 << BITS_PER_Y) - 32;
	public static final int MAX_Y = (Y_SIZE >> 1) - 1;
	public static final int MIN_Y = MAX_Y - Y_SIZE + 1;

	public final int x;
	public final int y;
	public final int z;

	public BlockPos(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public BlockPos add(int x, int y, int z) {
		return x == 0 && y == 0 && z == 0 ? this : new BlockPos(this.x + x, this.y + y, this.z + z);
	}

	public double distanceSquared(double x, double y, double z) {
		double dx = this.x - x;
		double dy = this.y - y;
		double dz = this.z - z;
		return dx * dx + dy * dy + dz * dz;
	}

	public ChunkPosition toChunkPosition(World world) {
		return new ChunkPosition(world, this.x >> 4, this.z >> 4);
	}

	public long toLong() {
		return (this.x & MASK_X) << OFFSET_X | (this.y & MASK_Y) << OFFSET_Y | (this.z & MASK_Z) << OFFSET_Z;
	}

	public static BlockPos fromLong(long value) {
		int x = (int) (value << (64 - BITS_PER_X - OFFSET_X) >> (64 - BITS_PER_X));
		int y = (int) (value << (64 - BITS_PER_Y - OFFSET_Y) >> (64 - BITS_PER_Y));
		int z = (int) (value << (64 - BITS_PER_Z - OFFSET_Z) >> (64 - BITS_PER_Z));
		return new BlockPos(x, y, z);
	}

	public int toSectionPos() {
		return (this.x & 0xF) << 12 | (this.y & 0xFFF) << 0 | (this.z & 0xF) << 16;
	}

	public static BlockPos fromSectionPos(int x, int z, int sectionPos) {
		x    += (sectionPos >> 12) & 0xF;
		int y = (sectionPos << 20 >> 20);
		z    += (sectionPos >> 16) & 0xF;
		return new BlockPos(x, y, z);
	}

	@Override
	public int compareTo(BlockPos other) {
		if (this.y == other.y) {
			if (this.z == other.z) {
				return this.x - other.x;
			}
			return this.z - other.z;
		}
		return this.y - other.y;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof BlockPos)) {
			return false;
		}
		BlockPos other = (BlockPos) obj;
		if (x != other.x) {
			return false;
		}
		if (y != other.y) {
			return false;
		}
		if (z != other.z) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + x;
		result = 31 * result + y;
		result = 31 * result + z;
		return result;
	}

	@Override
	public String toString() {
		return "BlockPos [x=" + x + ", y=" + y + ", z=" + z + "]";
	}
}
