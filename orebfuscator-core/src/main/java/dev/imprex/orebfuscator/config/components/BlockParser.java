package dev.imprex.orebfuscator.config.components;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import dev.imprex.orebfuscator.config.context.ConfigMessage;
import dev.imprex.orebfuscator.config.context.ConfigParsingContext;
import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.util.BlockProperties;
import dev.imprex.orebfuscator.util.BlockTag;

public class BlockParser {

  public static BlockParser.Factory factory(RegistryAccessor registryAccessor) {
    return new Factory(registryAccessor);
  }

  private final @NotNull RegistryAccessor registry;
  private final boolean excludeAir;

  private BlockParser(@NotNull RegistryAccessor registry, boolean excludeAir) {
    this.registry = Objects.requireNonNull(registry);
    this.excludeAir = excludeAir;
  }

  @NotNull
  public ConfigBlockValue parse(@NotNull ConfigParsingContext context, @NotNull String value) {
    var parsed = ConfigFunctionValue.parse(value);
    if (parsed != null) {
      return switch (parsed.function()) {
        case "tag" -> parseBlockTag(context, parsed.argument());
        default -> {
          context.warn(ConfigMessage.FUNCTION_UNKNOWN, parsed.function(), parsed.argument());
          yield ConfigBlockValue.invalid(value);
        }
      };
    } else {
      return parseBlock(context, value);
    }
  }

  @NotNull
  private ConfigBlockValue parseBlockTag(@NotNull ConfigParsingContext context, @NotNull String value) {
    BlockTag tag = registry.getBlockTagByName(value);
    if (tag == null) {
      context.warn(ConfigMessage.BLOCK_TAG_UNKNOWN, value);
      return ConfigBlockValue.invalidTag(value);
    }

    Set<BlockProperties> blocks = tag.blocks();
    if (blocks.isEmpty()) {
      context.warn(ConfigMessage.BLOCK_TAG_EMPTY, value);
      return ConfigBlockValue.invalidTag(value);
    }

    if (excludeAir) {
      // copy to mutable set
      blocks = new HashSet<>(blocks);

      for (var iterator = blocks.iterator(); iterator.hasNext(); ) {
        BlockProperties block = iterator.next();

        if (block.getDefaultBlockState().isAir()) {
          context.warn(ConfigMessage.BLOCK_TAG_AIR_BLOCK, block.getKey(), value);
          iterator.remove();
        }
      }

      if (blocks.isEmpty()) {
        context.warn(ConfigMessage.BLOCK_TAG_AIR_ONLY, value);
        return ConfigBlockValue.invalidTag(value);
      }
    }

    return ConfigBlockValue.tag(tag, blocks);
  }

  @NotNull
  private ConfigBlockValue parseBlock(@NotNull ConfigParsingContext context, @NotNull String value) {
    BlockProperties block = registry.getBlockByName(value);
    if (block == null) {
      context.warn(ConfigMessage.BLOCK_UNKNOWN, value);
    } else if (excludeAir && block.getDefaultBlockState().isAir()) {
      context.warn(ConfigMessage.BLOCK_AIR, value);
    } else {
      return ConfigBlockValue.block(block);
    }

    return ConfigBlockValue.invalid(value);
  }

  public static class Factory {

    private final @NotNull RegistryAccessor registry;

    private final BlockParser excludeAir;
    private final BlockParser includeAir;

    public Factory(@NotNull RegistryAccessor registry) {
      this.registry = Objects.requireNonNull(registry);

      this.excludeAir = new BlockParser(registry, true);
      this.includeAir = new BlockParser(registry, false);
    }

    public BlockParser excludeAir() {
      return excludeAir;
    }

    public BlockParser includeAir() {
      return includeAir;
    }
  }
}
