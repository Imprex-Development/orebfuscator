package dev.imprex.orebfuscator.proximity;

import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.EntityPose;
import dev.imprex.orebfuscator.util.QuickMaths;

public class ProximityRayCaster {

  private final RegistryAccessor registry;
  private final WorldAccessor level;
  private final boolean onlyCheckCenter;

  public ProximityRayCaster(RegistryAccessor registry, WorldAccessor level) {
    this.registry = registry;
    this.level = level;
    this.onlyCheckCenter = level.config().proximity().rayCastCheckOnlyCheckCenter();
  }

  public boolean isVisible(EntityPose origin, BlockPos target) {
    if (this.onlyCheckCenter) {
      return canRayPass(origin, target, 0.5, 0.5, 0.5);
    }

    // midfaces (6)
    return canRayPass(origin, target, 0.0, 0.5, 0.5)
        || canRayPass(origin, target, 0.5, 0.0, 0.5)
        || canRayPass(origin, target, 0.5, 0.5, 0.0)
        || canRayPass(origin, target, 0.5, 1.0, 0.5)
        || canRayPass(origin, target, 0.5, 0.5, 1.0)
        || canRayPass(origin, target, 1.0, 0.5, 0.5)

        // corners (8)
        || canRayPass(origin, target, 0.0, 0.0, 0.0)
        || canRayPass(origin, target, 1.0, 0.0, 0.0)
        || canRayPass(origin, target, 0.0, 1.0, 0.0)
        || canRayPass(origin, target, 1.0, 1.0, 0.0)
        || canRayPass(origin, target, 0.0, 0.0, 1.0)
        || canRayPass(origin, target, 1.0, 0.0, 1.0)
        || canRayPass(origin, target, 0.0, 1.0, 1.0)
        || canRayPass(origin, target, 1.0, 1.0, 1.0);
  }

  private boolean canRayPass(EntityPose origin, BlockPos target, double offsetX, double offsetY, double offsetZ) {
    double tx = target.x() + offsetX;
    double ty = target.y() + offsetY;
    double tz = target.z() + offsetZ;

    double dx = origin.x() - tx;
    double dy = origin.y() - ty;
    double dz = origin.z() - tz;

    double maxAbs = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
    // on top / inside
    if (maxAbs < 1) {
      return true;
    }

    // step in "dominant-axis" units
    dx /= maxAbs;
    dy /= maxAbs;
    dz /= maxAbs;

    // our current position
    double cx = origin.x();
    double cy = origin.y();
    double cz = origin.z();

    // position of current block
    int x, y, z;

    for (int steps = (int) Math.ceil(maxAbs); steps > 0; steps--) {
      // move from origin toward target
      cx -= dx;
      cy -= dy;
      cz -= dz;

      x = (int) QuickMaths.floor(cx);
      y = (int) QuickMaths.floor(cy);
      z = (int) QuickMaths.floor(cz);

      // check if we reached our target block
      if (x == target.x() && y == target.y() && z == target.z()) {
        return true;
      }

      int blockId = level.getBlockState(x, y, z);
      // fail on first hit, this ray is "blocked"
      if (registry.isOccluding(blockId)) {
        return false;
      }
    }

    return true;
  }
}
