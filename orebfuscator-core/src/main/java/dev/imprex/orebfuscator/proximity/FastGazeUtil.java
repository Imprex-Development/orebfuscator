package dev.imprex.orebfuscator.proximity;

import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.EntityPose;

// TODO: convert to instance class ProximityRayCaster with instance variables, etc.
public class FastGazeUtil {

  /**
   * Basic idea here is to take some rays from the considered block to the
   * level's eyes, and decide if any of those rays can reach the eyes unimpeded.
   *
   * @param pos             the starting block position
   * @param eyes            the destination eyes
   * @param level          the level world we are testing for
   * @param onlyCheckCenter only check block center if true
   * @return true if unimpeded path, false otherwise
   */
  public static boolean doFastCheck(
    BlockPos pos, EntityPose eyes,
    WorldAccessor level, RegistryAccessor registry,
    boolean onlyCheckCenter) {
      double ex = eyes.x();
      double ey = eyes.y();
      double ez = eyes.z();
      double x = pos.x();
      double y = pos.y();
      double z = pos.z();
      if (onlyCheckCenter) {
          return // center
              FastGazeUtil.fastAABBRayCheck(x, y, z, x + 0.5, y + 0.5, z + 0.5, ex, ey, ez, level, registry);
      } else {
          return // midfaces
              FastGazeUtil.fastAABBRayCheck(x, y, z, x, y + 0.5, z + 0.5, ex, ey, ez, level, registry)
                  || FastGazeUtil.fastAABBRayCheck(x, y, z, x + 0.5, y, z + 0.5, ex, ey, ez, level, registry)
                  || FastGazeUtil.fastAABBRayCheck(x, y, z, x + 0.5, y + 0.5, z, ex, ey, ez, level, registry)
                  || FastGazeUtil.fastAABBRayCheck(x, y, z, x + 0.5, y + 1.0, z + 0.5, ex, ey, ez, level, registry)
                  || FastGazeUtil.fastAABBRayCheck(x, y, z, x + 0.5, y + 0.5, z + 1.0, ex, ey, ez, level, registry)
                  || FastGazeUtil.fastAABBRayCheck(x, y, z, x + 1.0, y + 0.5, z + 0.5, ex, ey, ez, level, registry) ||
                  // corners
                  FastGazeUtil.fastAABBRayCheck(x, y, z, x, y, z, ex, ey, ez, level, registry)
                  || FastGazeUtil.fastAABBRayCheck(x, y, z, x + 1, y, z, ex, ey, ez, level, registry)
                  || FastGazeUtil.fastAABBRayCheck(x, y, z, x, y + 1, z, ex, ey, ez, level, registry)
                  || FastGazeUtil.fastAABBRayCheck(x, y, z, x + 1, y + 1, z, ex, ey, ez, level, registry)
                  || FastGazeUtil.fastAABBRayCheck(x, y, z, x, y, z + 1, ex, ey, ez, level, registry)
                  || FastGazeUtil.fastAABBRayCheck(x, y, z, x + 1, y, z + 1, ex, ey, ez, level, registry)
                  || FastGazeUtil.fastAABBRayCheck(x, y, z, x, y + 1, z + 1, ex, ey, ez, level, registry)
                  || FastGazeUtil.fastAABBRayCheck(x, y, z, x + 1, y + 1, z + 1, ex, ey, ez, level, registry);
      }
  }

  public static boolean fastAABBRayCheck(
    double bx, double by, double bz,
    double x, double y, double z,
    double ex, double ey, double ez,
    WorldAccessor level, RegistryAccessor registry) {
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
          int blockId = level.getBlockState((int) lx, (int) ly, (int) lz);
          if (blockId != 0 && registry.isOccluding(blockId)) {
              return false; // fail on first hit, this ray is "blocked"
          }
          s--; // we stop
      }
      return true;
  }
}
