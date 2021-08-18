package net.imprex.orebfuscator.util;

import org.bukkit.Location;
import org.bukkit.World;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.nms.BlockStateHolder;

public class MathUtil {

	public static int ceilToPowerOfTwo(int value) {
		value--;
		value |= value >> 1;
		value |= value >> 2;
		value |= value >> 4;
		value |= value >> 8;
		value |= value >> 16;
		value++;
		return value;
	}

	/**
	 * Basic idea here is to take some rays from the considered block to the
	 * player's eyes, and decide if any of those rays can reach the eyes unimpeded.
	 *
	 * @param block  the starting block
	 * @param eyes   the destination eyes
	 * @param player the player world we are testing for
	 * @return true if unimpeded path, false otherwise
	 */
	public static boolean doFastCheck(Location block, Location eyes, World player) {
		double ex = eyes.getX();
		double ey = eyes.getY();
		double ez = eyes.getZ();
		double x = block.getBlockX();
		double y = block.getBlockY();
		double z = block.getBlockZ();
		return // midfaces
		MathUtil.fastAABBRayCheck(x, y, z, x, y + 0.5, z + 0.5, ex, ey, ez, player)
				|| MathUtil.fastAABBRayCheck(x, y, z, x + 0.5, y, z + 0.5, ex, ey, ez, player)
				|| MathUtil.fastAABBRayCheck(x, y, z, x + 0.5, y + 0.5, z, ex, ey, ez, player)
				|| MathUtil.fastAABBRayCheck(x, y, z, x + 0.5, y + 1.0, z + 0.5, ex, ey, ez, player)
				|| MathUtil.fastAABBRayCheck(x, y, z, x + 0.5, y + 0.5, z + 1.0, ex, ey, ez, player)
				|| MathUtil.fastAABBRayCheck(x, y, z, x + 1.0, y + 0.5, z + 0.5, ex, ey, ez, player) ||
				// corners
				MathUtil.fastAABBRayCheck(x, y, z, x, y, z, ex, ey, ez, player)
				|| MathUtil.fastAABBRayCheck(x, y, z, x + 1, y, z, ex, ey, ez, player)
				|| MathUtil.fastAABBRayCheck(x, y, z, x, y + 1, z, ex, ey, ez, player)
				|| MathUtil.fastAABBRayCheck(x, y, z, x + 1, y + 1, z, ex, ey, ez, player)
				|| MathUtil.fastAABBRayCheck(x, y, z, x, y, z + 1, ex, ey, ez, player)
				|| MathUtil.fastAABBRayCheck(x, y, z, x + 1, y, z + 1, ex, ey, ez, player)
				|| MathUtil.fastAABBRayCheck(x, y, z, x, y + 1, z + 1, ex, ey, ez, player)
				|| MathUtil.fastAABBRayCheck(x, y, z, x + 1, y + 1, z + 1, ex, ey, ez, player);
	}

	public static boolean fastAABBRayCheck(double bx, double by, double bz, double x, double y, double z, double ex,
			double ey, double ez, World world) {
		double fx = ex - x;
		double fy = ey - y;
		double fz = ez - z;
		double absFx = Math.abs(fx);
		double absFy = Math.abs(fy);
		double absFz = Math.abs(fz);
		double s = Math.max(absFx, Math.max(absFy, absFz));

		if (s < 1) {
			return true; // on top / inside
		}

		double lx, ly, lz;

		fx = fx / s; // units of change along vector
		fy = fy / s;
		fz = fz / s;

		while (s > 0) {
			ex = ex - fx; // move along vector, we start _at_ the eye and move towards b
			ey = ey - fy;
			ez = ez - fz;
			lx = Math.floor(ex);
			ly = Math.floor(ey);
			lz = Math.floor(ez);
			if (lx == bx && ly == by && lz == bz) {
				return true; // we've reached our starting block, don't test it.
			}
			BlockStateHolder between = NmsInstance.getBlockState(world, (int) lx, (int) ly, (int) lz);
			if (between != null && !MaterialUtil.isTransparent(between.getBlockId())) {
				return false; // fail on first hit, this ray is "blocked"
			}
			s--; // we stop
		}
		return true;
	}
}