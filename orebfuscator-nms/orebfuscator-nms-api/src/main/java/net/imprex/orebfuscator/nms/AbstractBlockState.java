package net.imprex.orebfuscator.nms;

import org.bukkit.World;

import net.imprex.orebfuscator.util.BlockPos;

public abstract class AbstractBlockState<T> implements BlockStateHolder {

	protected final World world;
	private final BlockPos position;

	protected final T state;

	public AbstractBlockState(int x, int y, int z, World world, T state) {
		this.world = world;
		this.position = new BlockPos(x, y, z);
		this.state = state;
	}

	@Override
	public World getWorld() {
		return this.world;
	}

	@Override
	public BlockPos getPosition() {
		return position;
	}

	@Override
	public int getX() {
		return this.position.x;
	}

	@Override
	public int getY() {
		return this.position.y;
	}

	@Override
	public int getZ() {
		return this.position.z;
	}

	@Override
	public String toString() {
		return "[" + this.world + ", " + this.position + ", " + this.state + "]";
	}
}
