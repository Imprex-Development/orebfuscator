package net.imprex.orebfuscator.util;

public enum ChunkDirection {

	NORTH(1, 0), EAST(0, 1), SOUTH(-1, 0), WEST(0, -1);

	private int offsetX;
	private int offsetZ;

	private ChunkDirection(int offsetX, int offsetZ) {
		this.offsetX = offsetX;
		this.offsetZ = offsetZ;
	}

	public int getOffsetX() {
		return offsetX;
	}

	public int getOffsetZ() {
		return offsetZ;
	}

	public static ChunkDirection fromPosition(ChunkPosition position, int targetX, int targetZ) {
		int offsetX = (targetX >> 4) - position.getX();
		int offsetZ = (targetZ >> 4) - position.getZ();

		if (offsetX == 1 && offsetZ == 0) {
			return NORTH;
		} else if (offsetX == 0 && offsetZ == 1) {
			return EAST;
		} else if (offsetX == -1 && offsetZ == 0) {
			return SOUTH;
		} else if (offsetX == 0 && offsetZ == -1) {
			return WEST;
		}

		throw new IllegalArgumentException();
	}
}
