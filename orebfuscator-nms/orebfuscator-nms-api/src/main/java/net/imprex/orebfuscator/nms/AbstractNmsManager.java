package net.imprex.orebfuscator.nms;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.imprex.orebfuscator.reflect.Reflector;
import dev.imprex.orebfuscator.reflect.accessor.MethodAccessor;
import dev.imprex.orebfuscator.util.BlockProperties;
import dev.imprex.orebfuscator.util.BlockStateProperties;
import dev.imprex.orebfuscator.util.BlockTag;
import dev.imprex.orebfuscator.util.MathUtil;
import dev.imprex.orebfuscator.util.NamespacedKey;

public abstract class AbstractNmsManager implements NmsManager {

  private static MethodAccessor worldGetHandle;
  private static MethodAccessor playerGetHandle;

  protected static <T> T worldHandle(World world, Class<T> targetClass) {
    if (worldGetHandle == null) {
      worldGetHandle = Reflector.of(world.getClass()).method()
          .banStatic()
          .nameIs("getHandle")
          .returnType().is(targetClass)
          .parameterCount(0)
          .firstOrThrow();
    }
    return targetClass.cast(worldGetHandle.invoke(world));
  }

  protected static <T> T playerHandle(Player player, Class<T> targetClass) {
    if (playerGetHandle == null) {
      playerGetHandle = Reflector.of(player.getClass()).method()
          .banStatic()
          .nameIs("getHandle")
          .returnType().is(targetClass)
          .parameterCount(0)
          .firstOrThrow();
    }
    return targetClass.cast(playerGetHandle.invoke(player));
  }

  private final int uniqueBlockStateCount;
  private final int maxBitsPerBlockState;

  private final BlockStateProperties[] blockStates;
  private final Map<NamespacedKey, BlockProperties> blocks = new HashMap<>();
  protected final Map<NamespacedKey, BlockTag> tags = new HashMap<>();

  public AbstractNmsManager(int uniqueBlockStateCount) {
    this.uniqueBlockStateCount = uniqueBlockStateCount;
    this.maxBitsPerBlockState = MathUtil.ceilLog2(uniqueBlockStateCount);

    this.blockStates = new BlockStateProperties[uniqueBlockStateCount];
  }

  protected final void registerBlockProperties(BlockProperties block) {
    this.blocks.put(block.getKey(), block);

    for (BlockStateProperties blockState : block.getBlockStates()) {
      this.blockStates[blockState.getId()] = blockState;
    }
  }

  protected final void registerBlockTag(BlockTag tag) {
    this.tags.put(tag.key(), tag);
  }

  @Override
  public final int getUniqueBlockStateCount() {
    return this.uniqueBlockStateCount;
  }

  @Override
  public final int getMaxBitsPerBlockState() {
    return this.maxBitsPerBlockState;
  }

  @Override
  public final @Nullable BlockProperties getBlockByName(@NotNull String name) {
    return this.blocks.get(NamespacedKey.fromString(name));
  }

  @Override
  public final @Nullable BlockTag getBlockTagByName(@NotNull String name) {
    return this.tags.get(NamespacedKey.fromString(name));
  }

  @Override
  public final boolean isAir(int id) {
    return this.blockStates[id].isAir();
  }

  @Override
  public final boolean isOccluding(int id) {
    return this.blockStates[id].isOccluding();
  }

  @Override
  public final boolean isBlockEntity(int id) {
    return this.blockStates[id].isBlockEntity();
  }
}
