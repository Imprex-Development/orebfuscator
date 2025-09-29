package dev.imprex.orebfuscator.util;

import java.util.Objects;

import org.bukkit.World;

public class ChunkPosition {

  public static long toLong(int chunkX, int chunkZ) {
    return (chunkZ & 0xffffffffL) << 32 | chunkX & 0xffffffffL;
  }

  public final String world;
  public final int x;
  public final int z;

  public ChunkPosition(World world, int x, int z) {
    this.world = Objects.requireNonNull(world).getName();
    this.x = x;
    this.z = z;
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = 31 * result + world.hashCode();
    result = 31 * result + x;
    result = 31 * result + z;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ChunkPosition)) {
      return false;
    }
    ChunkPosition other = (ChunkPosition) obj;
    return x == other.x && z == other.z && Objects.equals(world, other.world);
  }

  @Override
  public String toString() {
    return "ChunkPosition [world=" + world + ", x=" + x + ", z=" + z + "]";
  }
}
