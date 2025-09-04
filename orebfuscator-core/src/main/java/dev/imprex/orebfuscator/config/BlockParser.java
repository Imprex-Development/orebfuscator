package dev.imprex.orebfuscator.config;

import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;
import dev.imprex.orebfuscator.config.components.ConfigBlockValue;
import dev.imprex.orebfuscator.config.components.ConfigFunctionValue;
import dev.imprex.orebfuscator.config.context.ConfigMessage;
import dev.imprex.orebfuscator.config.context.ConfigParsingContext;
import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.util.BlockProperties;
import dev.imprex.orebfuscator.util.BlockTag;

public class BlockParser {

  @NotNull
  public static ConfigBlockValue parseBlockOrBlockTag(
      @NotNull RegistryAccessor registry,
      @NotNull ConfigParsingContext context,
      @NotNull String value,
      boolean excludeAir
  ) {
    var parsed = ConfigFunctionValue.parse(value);
    if (parsed != null) {
      return switch (parsed.function()) {
        case "tag" -> parseBlockTag(registry, context, parsed.argument(), excludeAir);
        default -> {
          context.warn(ConfigMessage.FUNCTION_UNKNOWN, parsed.function(), parsed.argument());
          yield ConfigBlockValue.invalid(value);
        }
      };
    } else {
      return parseBlock(registry, context, value, excludeAir);
    }
  }

  @NotNull
  private static ConfigBlockValue parseBlockTag(
      @NotNull RegistryAccessor registry,
      @NotNull ConfigParsingContext context,
      @NotNull String value,
      boolean excludeAir
  ) {
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
  private static ConfigBlockValue parseBlock(
      @NotNull RegistryAccessor registry,
      @NotNull ConfigParsingContext context,
      @NotNull String value,
      boolean excludeAir
  ) {
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
}
