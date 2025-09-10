package dev.imprex.orebfuscator.config.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import dev.imprex.orebfuscator.config.context.ConfigMessage;
import dev.imprex.orebfuscator.config.context.ConfigParsingContext;
import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.BlockStateProperties;
import dev.imprex.orebfuscator.util.MathUtil;
import dev.imprex.orebfuscator.util.WeightedRandom;

public class WeightedBlockList {

  public static WeightedRandom[] create(WorldAccessor world, List<WeightedBlockList> lists) {
    WeightedRandom[] heightMap = new WeightedRandom[world.getHeight()];

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
        WeightedRandom.Builder builder = WeightedRandom.builder();

        for (WeightedBlockList list : next) {
          for (Map.Entry<ConfigBlockValue, Integer> entry : list.blocks.entrySet()) {
            // TODO: add support for other block states in future
            var blockStates = entry.getKey().blocks().stream()
                .map(block -> block.getDefaultBlockState())
                .collect(Collectors.toSet());
            double weight = (double) entry.getValue() / (double) blockStates.size();

            for (BlockStateProperties state : blockStates) {
              builder.add(state.getId(), weight);
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

  public WeightedBlockList(BlockParser.Factory blockParserFactory, ConfigurationSection section,
      ConfigParsingContext context) {
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
    
    final BlockParser parser = blockParserFactory.includeAir();

    boolean isEmpty = true;
    for (String value : blocksSection.getKeys()) {
      int weight = blocksSection.getInt(value, 1);
      var parsed = parser.parse(context, value);
      this.blocks.put(parsed, weight);
      isEmpty &= parsed.blocks().isEmpty();
    }

    if (isEmpty) {
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
