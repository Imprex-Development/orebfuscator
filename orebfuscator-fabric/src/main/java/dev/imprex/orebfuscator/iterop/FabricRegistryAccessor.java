package dev.imprex.orebfuscator.iterop;

import com.google.common.collect.ImmutableList;
import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.util.BlockProperties;
import dev.imprex.orebfuscator.util.BlockStateProperties;
import dev.imprex.orebfuscator.util.BlockTag;
import dev.imprex.orebfuscator.util.NamespacedKey;
import dev.imprex.orebfuscator.util.QuickMaths;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class FabricRegistryAccessor implements RegistryAccessor {

  private final int uniqueBlockStateCount;
  private final int maxBitsPerBlockState;

  private final BlockStateProperties[] blockStates;
  private final Map<NamespacedKey, BlockProperties> blocks = new HashMap<>();
  protected final Map<NamespacedKey, BlockTag> tags = new HashMap<>();

  public FabricRegistryAccessor() {
    this.uniqueBlockStateCount = Block.BLOCK_STATE_REGISTRY.size();
    this.maxBitsPerBlockState = QuickMaths.ceilLog2(uniqueBlockStateCount);

    this.blockStates = new BlockStateProperties[uniqueBlockStateCount];
    for (Map.Entry<ResourceKey<Block>, Block> entry : BuiltInRegistries.BLOCK.entrySet()) {
      NamespacedKey namespacedKey = NamespacedKey.parse(entry.getKey().identifier().toString());
      Block block = entry.getValue();

      ImmutableList<BlockState> possibleBlockStates = block.getStateDefinition().getPossibleStates();
      BlockProperties.Builder builder = BlockProperties.builder(namespacedKey);

      for (BlockState blockState : possibleBlockStates) {
        BlockStateProperties properties =
            BlockStateProperties.builder(Block.getId(blockState)).withIsAir(blockState.isAir())
                .withIsFluid(!blockState.getFluidState().isEmpty()).withIsLava(block == Blocks.LAVA)
                .withIsOccluding(blockState.isSolidRender()).withIsBlockEntity(blockState.hasBlockEntity())
                .withIsDefaultState(Objects.equals(block.defaultBlockState(), blockState)).build();

        builder.withBlockState(properties);
        this.blockStates[properties.getId()] = properties;
      }

      this.blocks.put(namespacedKey, builder.build());
    }

    BuiltInRegistries.BLOCK.getTags().forEach(tag -> {
      NamespacedKey namespacedKey = NamespacedKey.parse(tag.key().location().toString());

      Set<BlockProperties> blocks =
          tag.stream().map(holder -> holder.unwrapKey().map(key -> getBlockByName(key.identifier().toString())))
              .filter(Optional::isPresent).map(Optional::get).collect(Collectors.toUnmodifiableSet());

      this.tags.put(namespacedKey, new BlockTag(namespacedKey, blocks));
    });
  }

  @Override
  public int getUniqueBlockStateCount() {
    return this.uniqueBlockStateCount;
  }

  @Override
  public int getMaxBitsPerBlockState() {
    return this.maxBitsPerBlockState;
  }

  @Override
  public final @Nullable BlockProperties getBlockByName(String name) {
    return NamespacedKey.tryParse(name).map(this.blocks::get).orElse(null);
  }

  @Override
  public final @Nullable BlockTag getBlockTagByName(String name) {
    return NamespacedKey.tryParse(name).map(this.tags::get).orElse(null);
  }

  @Override
  public final boolean isAir(int id) {
    return this.blockStates[id].isAir();
  }

  @Override
  public boolean isFluid(int blockId) {
    return this.blockStates[blockId].isFluid();
  }

  @Override
  public boolean isLava(int id) {
    return this.blockStates[id].isLava();
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
