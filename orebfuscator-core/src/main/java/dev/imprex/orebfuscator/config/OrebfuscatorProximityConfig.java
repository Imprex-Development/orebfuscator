package dev.imprex.orebfuscator.config;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.joml.Matrix4f;
import dev.imprex.orebfuscator.config.api.BlockFlags;
import dev.imprex.orebfuscator.config.api.ProximityConfig;
import dev.imprex.orebfuscator.config.components.ConfigBlockValue;
import dev.imprex.orebfuscator.config.components.WeightedBlockList;
import dev.imprex.orebfuscator.config.context.ConfigMessage;
import dev.imprex.orebfuscator.config.context.ConfigParsingContext;
import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.util.BlockProperties;

public class OrebfuscatorProximityConfig extends AbstractWorldConfig implements ProximityConfig {

  private int distance = 24;

  private boolean frustumCullingEnabled = true;
  private float frustumCullingMinDistance = 3;
  private float frustumCullingFov = 80f;

  private float frustumCullingMinDistanceSquared = 9;
  private final Matrix4f frustumCullingProjectionMatrix;

  private boolean rayCastCheckEnabled = false;
  private boolean rayCastCheckOnlyCheckCenter = false;
  private int defaultBlockFlags = (ProximityHeightCondition.MATCH_ALL | BlockFlags.FLAG_USE_BLOCK_BELOW);

  private boolean usesBlockSpecificConfigs = false;
  private final Map<ConfigBlockValue, Integer> hiddenBlocks = new LinkedHashMap<>();
  private final Set<BlockProperties> allowForUseBlockBelow = new HashSet<>();

  OrebfuscatorProximityConfig(RegistryAccessor registry, ConfigurationSection section, ConfigParsingContext context) {
    super(section.getName());
    this.deserializeBase(section, context);

    this.distance = section.getInt("distance", 24);
    context.errorMinValue("distance", 1, this.distance);

    this.frustumCullingEnabled = section.getBoolean("frustumCulling.enabled", false);
    this.frustumCullingMinDistance = section.getDouble("frustumCulling.minDistance", 3d).floatValue();
    this.frustumCullingFov = section.getDouble("frustumCulling.fov", 80d).floatValue();

    if (this.frustumCullingEnabled) {
      context.errorMinMaxValue("frustumCulling.fov", 10, 170, (int) this.frustumCullingFov);
    }

    this.frustumCullingMinDistanceSquared = this.frustumCullingMinDistance * this.frustumCullingMinDistance;
    this.frustumCullingProjectionMatrix = new Matrix4f() // create projection matrix with aspect 16:9
        .perspective(this.frustumCullingFov, 16f / 9f, 0.01f, 2 * this.distance);

    this.rayCastCheckEnabled = section.getBoolean("rayCastCheck.enabled", false);
    this.rayCastCheckOnlyCheckCenter = section.getBoolean("rayCastCheck.onlyCheckCenter", false);

    this.defaultBlockFlags = ProximityHeightCondition.create(this.minY, this.maxY);
    if (section.getBoolean("useBlockBelow", true)) {
      this.defaultBlockFlags |= BlockFlags.FLAG_USE_BLOCK_BELOW;
    }

    this.deserializeHiddenBlocks(registry, section, context);
    this.deserializeRandomBlocks(registry, section, context);

    for (WeightedBlockList blockList : this.weightedBlockLists) {
      this.allowForUseBlockBelow.addAll(blockList.getBlocks().stream()
          .flatMap(value -> value.blocks().stream())
          .toList());
    }

    this.disableOnError(context);
  }

  protected void serialize(ConfigurationSection section) {
    this.serializeBase(section);

    section.set("distance", this.distance);

    section.set("frustumCulling.enabled", this.frustumCullingEnabled);
    section.set("frustumCulling.minDistance", this.frustumCullingMinDistance);
    section.set("frustumCulling.fov", this.frustumCullingFov);

    section.set("rayCastCheck.enabled", this.rayCastCheckEnabled);
    section.set("rayCastCheck.onlyCheckCenter", this.rayCastCheckOnlyCheckCenter);
    section.set("useBlockBelow", BlockFlags.isUseBlockBelowBitSet(this.defaultBlockFlags));

    this.serializeHiddenBlocks(section);
    this.serializeRandomBlocks(section);
  }

  private void deserializeHiddenBlocks(RegistryAccessor registry, ConfigurationSection section,
      ConfigParsingContext context) {
    context = context.section("hiddenBlocks");

    ConfigurationSection blockSection = section.getSection("hiddenBlocks");
    if (blockSection == null) {
      return;
    }

    boolean isEmpty = true;
    for (ConfigurationSection block : blockSection.getSubSections()) {
      ConfigBlockValue parsed = BlockParser.parseBlockOrBlockTag(registry, context, block.getName(), true);

      int blockFlags = this.defaultBlockFlags;

      // parse block specific height condition
      if (block.isNumber("minY") && block.isNumber("maxY")) {
        int minY = block.getInt("minY", this.minY);
        int maxY = block.getInt("maxY", this.maxY);

        blockFlags = ProximityHeightCondition.remove(blockFlags);
        blockFlags |= ProximityHeightCondition.create(
            Math.min(minY, maxY),
            Math.max(minY, maxY));
        usesBlockSpecificConfigs = true;
      }

      // parse block specific flags
      if (block.isBoolean("useBlockBelow")) {
        if (block.getBoolean("useBlockBelow", true)) {
          blockFlags |= BlockFlags.FLAG_USE_BLOCK_BELOW;
        } else {
          blockFlags &= ~BlockFlags.FLAG_USE_BLOCK_BELOW;
        }
        usesBlockSpecificConfigs = true;
      }

      this.hiddenBlocks.put(parsed, blockFlags);
      isEmpty &= parsed.blocks().isEmpty();
    }

    if (isEmpty) {
      context.error(ConfigMessage.MISSING_OR_EMPTY);
    }
  }

  private void serializeHiddenBlocks(ConfigurationSection section) {
    ConfigurationSection blockSection = section.createSection("hiddenBlocks");

    for (Map.Entry<ConfigBlockValue, Integer> entry : this.hiddenBlocks.entrySet()) {
      ConfigurationSection block = blockSection.createSection(entry.getKey().value());

      int blockFlags = entry.getValue();
      if (!ProximityHeightCondition.equals(blockFlags, this.defaultBlockFlags)) {
        block.set("minY", ProximityHeightCondition.getMinY(blockFlags));
        block.set("maxY", ProximityHeightCondition.getMaxY(blockFlags));
      }

      if (BlockFlags.isUseBlockBelowBitSet(blockFlags) != BlockFlags.isUseBlockBelowBitSet(
          this.defaultBlockFlags)) {
        block.set("useBlockBelow", BlockFlags.isUseBlockBelowBitSet(blockFlags));
      }
    }
  }

  @Override
  public int distance() {
    return this.distance;
  }

  @Override
  public boolean frustumCullingEnabled() {
    return this.frustumCullingEnabled;
  }

  @Override
  public float frustumCullingMinDistanceSquared() {
    return this.frustumCullingMinDistanceSquared;
  }

  @Override
  public Matrix4f frustumCullingProjectionMatrix() {
    return new Matrix4f(frustumCullingProjectionMatrix);
  }

  @Override
  public boolean rayCastCheckEnabled() {
    return this.rayCastCheckEnabled;
  }

  @Override
  public boolean rayCastCheckOnlyCheckCenter() {
    return this.rayCastCheckOnlyCheckCenter;
  }

  @Override
  public Iterable<Map.Entry<ConfigBlockValue, Integer>> hiddenBlocks() {
    return this.hiddenBlocks.entrySet();
  }

  public Iterable<BlockProperties> allowForUseBlockBelow() {
    return this.allowForUseBlockBelow;
  }

  boolean usesBlockSpecificConfigs() {
    return usesBlockSpecificConfigs;
  }
}
