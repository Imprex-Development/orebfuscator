package dev.imprex.orebfuscator.config.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import dev.imprex.orebfuscator.config.context.ConfigMessage;
import dev.imprex.orebfuscator.config.context.ConfigParsingContext;
import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.BlockProperties;
import dev.imprex.orebfuscator.util.MathUtil;
import dev.imprex.orebfuscator.util.WeightedIntRandom;

public class WeightedBlockList {

  public static WeightedIntRandom[] create(WorldAccessor world, List<WeightedBlockList> lists) {
    WeightedIntRandom[] heightMap = new WeightedIntRandom[world.getHeight()];

    List<WeightedBlockList> last = new ArrayList<>();
    List<WeightedBlockList> next = new ArrayList<>();

    int count = 0;

    for (int y = world.getMinBuildHeight(); y < world.getMaxBuildHeight(); y++) {
      for (WeightedBlockList list : lists) {
        if (list.minY <= y && list.maxY >= y) {
          next.add(list);
        }
      }

      int index = y - world.getMinBuildHeight();
      if (index > 0 && last.equals(next)) {
        // copy last weighted random
        heightMap[index] = heightMap[index - 1];
      } else {
        WeightedIntRandom.Builder builder = WeightedIntRandom.builder();

        for (WeightedBlockList list : next) {
          for (Map.Entry<ConfigBlockValue, Integer> entry : list.blocks.entrySet()) {
            for (BlockProperties block : entry.getKey().blocks()) {
              if (!builder.add(block.getDefaultBlockState().getId(), entry.getValue())) {
                OfcLogger.warn(String.format("duplicate randomBlock entry for %s in %s", block.getKey(),
                    list.name));
              }
            }
          }
        }

        heightMap[index] = builder.build();
        count++;

        // only update last if recomputed
        last.clear();
        last.addAll(next);
      }

      next.clear();
    }

    OfcLogger.debug(String.format("Successfully created %s weighted randoms", count));

    return heightMap;
  }

  private final String name;

  private final int minY;
  private final int maxY;

  private final Map<ConfigBlockValue, Integer> blocks = new LinkedHashMap<>();

  public WeightedBlockList(RegistryAccessor registry, ConfigurationSection section, ConfigParsingContext context) {
    this.name = section.getName();

    int minY = MathUtil.clamp(section.getInt("minY", BlockPos.MIN_Y), BlockPos.MIN_Y, BlockPos.MAX_Y);
    int maxY = MathUtil.clamp(section.getInt("maxY", BlockPos.MAX_Y), BlockPos.MIN_Y, BlockPos.MAX_Y);

    this.minY = Math.min(minY, maxY);
    this.maxY = Math.max(minY, maxY);

    ConfigParsingContext blocksContext = context.section("blocks");
    ConfigurationSection blocksSection = section.getSection("blocks");
    if (blocksSection == null) {
      blocksContext.error(ConfigMessage.MISSING_OR_EMPTY);
      return;
    }

    for (String blockName : blocksSection.getKeys()) {
      BlockProperties blockProperties = registry.getBlockByName(blockName);
      if (blockProperties != null) {
        int weight = blocksSection.getInt(blockName, 1);
        this.blocks.put(ConfigBlockValue.block(blockProperties), weight);
      } else {
        this.blocks.put(ConfigBlockValue.invalid(blockName), 1);
        blocksContext.warn(ConfigMessage.BLOCK_UNKNOWN, blockName);
      }
    }

    // TODO: ignore invalid values in this check
    if (this.blocks.isEmpty()) {
      blocksContext.error(ConfigMessage.MISSING_OR_EMPTY);
    }
  }

  public void serialize(ConfigurationSection section) {
    section = section.createSection(this.name);

    section.set("minY", this.minY);
    section.set("maxY", this.maxY);

    section = section.createSection("blocks");
    for (Map.Entry<ConfigBlockValue, Integer> entry : this.blocks.entrySet()) {
      section.set(entry.getKey().value(), entry.getValue());
    }
  }

  public Set<ConfigBlockValue> getBlocks() {
    return Collections.unmodifiableSet(this.blocks.keySet());
  }
}
