package dev.imprex.orebfuscator.util;

import org.jspecify.annotations.Nullable;
import dev.imprex.orebfuscator.interop.WorldAccessor;

// TODO: nullability
public record EntityPose(@Nullable WorldAccessor world, double x, double y, double z, float rotX, float rotY) {

  public static final EntityPose ZERO = new EntityPose(null, 0, 0, 0, 0, 0);

  public int blockX() {
    return MathUtil.floor(this.x);
  }

  public int blockY() {
    return MathUtil.floor(this.y);
  }

  public int blockZ() {
    return MathUtil.floor(this.z);
  }

  public double distanceSquared(EntityPose other) {
    double dx = this.x - other.x;
    double dy = this.y - other.y;
    double dz = this.z - other.z;
    return dx * dx + dy * dy + dz * dz;
  }
}
