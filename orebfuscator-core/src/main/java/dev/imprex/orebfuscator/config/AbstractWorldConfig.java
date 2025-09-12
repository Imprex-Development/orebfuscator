package dev.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.google.gson.JsonObject;
import dev.imprex.orebfuscator.config.api.WorldConfig;
import dev.imprex.orebfuscator.config.components.BlockParser;
import dev.imprex.orebfuscator.config.components.ConfigBlockValue;
import dev.imprex.orebfuscator.config.components.WeightedBlockList;
import dev.imprex.orebfuscator.config.components.WorldMatcher;
import dev.imprex.orebfuscator.config.context.ConfigMessage;
import dev.imprex.orebfuscator.config.context.ConfigParsingContext;
import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.MathUtil;
import dev.imprex.orebfuscator.util.WeightedRandom;

public abstract class AbstractWorldConfig implements WorldConfig {

  private final String name;

  protected boolean enabledValue = false;
  protected boolean enabled = false;

  protected int minY = BlockPos.MIN_Y;
  protected int maxY = BlockPos.MAX_Y;

  protected final List<WorldMatcher> worldMatchers = new ArrayList<>();
  protected final List<WeightedBlockList> weightedBlockLists = new ArrayList<>();

  public AbstractWorldConfig(String name) {
    this.name = name;
  }

  protected void deserializeBase(ConfigurationSection section, ConfigParsingContext context) {
    this.enabledValue = section.getBoolean("enabled", true);

    int minY = MathUtil.clamp(section.getInt("minY", BlockPos.MIN_Y), BlockPos.MIN_Y, BlockPos.MAX_Y);
    int maxY = MathUtil.clamp(section.getInt("maxY", BlockPos.MAX_Y), BlockPos.MIN_Y, BlockPos.MAX_Y);

    this.minY = Math.min(minY, maxY);
    this.maxY = Math.max(minY, maxY);

    section.getStringList("worlds").stream()
        .map(WorldMatcher::parseMatcher)
        .forEach(worldMatchers::add);

    if (this.worldMatchers.isEmpty()) {
      context.section("worlds").error(ConfigMessage.MISSING_OR_EMPTY);
    }
  }

  protected void serializeBase(ConfigurationSection section) {
    section.set("enabled", this.enabledValue);

    section.set("minY", this.minY);
    section.set("maxY", this.maxY);

    section.set("worlds", worldMatchers.stream()
        .map(WorldMatcher::serialize)
        .collect(Collectors.toList()));
  }

  protected void deserializeRandomBlocks(BlockParser.Factory blockParserFactory, ConfigurationSection section,
      ConfigParsingContext context) {
    context = context.section("randomBlocks");

    ConfigurationSection subSectionContainer = section.getSection("randomBlocks");
    if (subSectionContainer == null) {
      context.error(ConfigMessage.MISSING_OR_EMPTY);
      return;
    }

    for (ConfigurationSection subSection : subSectionContainer.getSubSections()) {
      ConfigParsingContext subContext = context.section(subSection.getName());
      this.weightedBlockLists.add(new WeightedBlockList(blockParserFactory, subSection, subContext));
    }

    if (this.weightedBlockLists.isEmpty()) {
      context.error(ConfigMessage.MISSING_OR_EMPTY);
    }
  }

  protected void serializeRandomBlocks(ConfigurationSection section) {
    ConfigurationSection subSectionContainer = section.createSection("randomBlocks");

    for (WeightedBlockList weightedBlockList : this.weightedBlockLists) {
      weightedBlockList.serialize(subSectionContainer);
    }
  }

  protected void disableOnError(ConfigParsingContext context) {
    this.enabled = context.disableIfError(this.enabledValue);
  }

  protected String getName() {
    return name;
  }

  public JsonObject randomBlocksToJson() {
    JsonObject object = new JsonObject();
    for (WeightedBlockList list : weightedBlockLists) {
      object.add(list.getName(), ConfigBlockValue.toJson(list.getBlocks()));
    }
    return object;
  }

  @Override
  public boolean isEnabled() {
    return this.enabled;
  }

  @Override
  public int getMinY() {
    return this.minY;
  }

  @Override
  public int getMaxY() {
    return this.maxY;
  }

  @Override
  public boolean matchesWorldName(String worldName) {
    for (WorldMatcher matcher : this.worldMatchers) {
      if (matcher.test(worldName)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean shouldObfuscate(int y) {
    return y >= this.minY && y <= this.maxY;
  }


  WeightedRandom[] createWeightedRandoms(WorldAccessor world) {
    OfcLogger.debug(String.format("Creating weighted randoms for %s for world %s:", name, world));
    return WeightedBlockList.create(world, this.weightedBlockLists);
  }
}
