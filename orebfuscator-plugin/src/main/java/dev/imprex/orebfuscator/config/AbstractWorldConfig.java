package dev.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.configuration.ConfigurationSection;

import dev.imprex.orebfuscator.config.api.WorldConfig;
import dev.imprex.orebfuscator.config.components.WeightedBlockList;
import dev.imprex.orebfuscator.config.components.WorldMatcher;
import dev.imprex.orebfuscator.config.context.ConfigParsingContext;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.MathUtil;
import dev.imprex.orebfuscator.util.WeightedIntRandom;
import net.imprex.orebfuscator.util.HeightAccessor;

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

  protected void deserializeBase(ConfigurationSection section) {
    this.enabledValue = section.getBoolean("enabled", true);

    int minY = MathUtil.clamp(section.getInt("minY", BlockPos.MIN_Y), BlockPos.MIN_Y, BlockPos.MAX_Y);
    int maxY = MathUtil.clamp(section.getInt("maxY", BlockPos.MAX_Y), BlockPos.MIN_Y, BlockPos.MAX_Y);

    this.minY = Math.min(minY, maxY);
    this.maxY = Math.max(minY, maxY);
  }

  protected void serializeBase(ConfigurationSection section) {
    section.set("enabled", this.enabledValue);
    section.set("minY", this.minY);
    section.set("maxY", this.maxY);
  }

  protected void deserializeWorlds(ConfigurationSection section, ConfigParsingContext context, String path) {
    context = context.section(path);

    section.getStringList(path).stream().map(WorldMatcher::parseMatcher).forEach(worldMatchers::add);

    if (this.worldMatchers.isEmpty()) {
      context.errorMissingOrEmpty();
    }
  }

  protected void serializeWorlds(ConfigurationSection section, String path) {
    section.set(path, worldMatchers.stream().map(WorldMatcher::serialize).collect(Collectors.toList()));
  }

  protected void deserializeRandomBlocks(ConfigurationSection section, ConfigParsingContext context, String path) {
    context = context.section(path);

    ConfigurationSection subSectionContainer = section.getConfigurationSection(path);
    if (subSectionContainer == null) {
      context.errorMissingOrEmpty();
      return;
    }

    for (String subSectionName : subSectionContainer.getKeys(false)) {
      ConfigParsingContext subContext = context.section(subSectionName);
      ConfigurationSection subSection = subSectionContainer.getConfigurationSection(subSectionName);
      this.weightedBlockLists.add(new WeightedBlockList(subSection, subContext));
    }

    if (this.weightedBlockLists.isEmpty()) {
      context.errorMissingOrEmpty();
    }
  }

  protected void serializeRandomBlocks(ConfigurationSection section, String path) {
    ConfigurationSection subSectionContainer = section.createSection(path);

    for (WeightedBlockList weightedBlockList : this.weightedBlockLists) {
      weightedBlockList.serialize(subSectionContainer);
    }
  }

  protected void disableOnError(ConfigParsingContext context) {
    this.enabled = context.disableIfError(this.enabledValue);
  }

  public String getName() {
    return name;
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


  WeightedIntRandom[] createWeightedRandoms(HeightAccessor heightAccessor) {
    OfcLogger.debug(String.format("Creating weighted randoms for %s for world %s:", name, heightAccessor));
    return WeightedBlockList.create(heightAccessor, this.weightedBlockLists);
  }
}
