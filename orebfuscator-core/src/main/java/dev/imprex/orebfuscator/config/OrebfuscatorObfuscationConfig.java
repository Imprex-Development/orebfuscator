package dev.imprex.orebfuscator.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import dev.imprex.orebfuscator.config.api.ObfuscationConfig;
import dev.imprex.orebfuscator.config.components.ConfigBlockValue;
import dev.imprex.orebfuscator.config.context.ConfigMessage;
import dev.imprex.orebfuscator.config.context.ConfigParsingContext;
import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import dev.imprex.orebfuscator.interop.RegistryAccessor;

public class OrebfuscatorObfuscationConfig extends AbstractWorldConfig implements ObfuscationConfig {

  private boolean layerObfuscation = false;

  private final Set<ConfigBlockValue> hiddenBlocks = new LinkedHashSet<>();

  OrebfuscatorObfuscationConfig(RegistryAccessor registry, ConfigurationSection section,
      ConfigParsingContext context) {
    super(section.getName());
    this.deserializeBase(section, context);

    this.layerObfuscation = section.getBoolean("layerObfuscation", false);

    this.deserializeHiddenBlocks(registry, section, context);
    this.deserializeRandomBlocks(registry, section, context);
    this.disableOnError(context);
  }

  void serialize(ConfigurationSection section) {
    this.serializeBase(section);

    section.set("layerObfuscation", this.layerObfuscation);

    this.serializeHiddenBlocks(section);
    this.serializeRandomBlocks(section);
  }

  private void deserializeHiddenBlocks(RegistryAccessor registry, ConfigurationSection section,
      ConfigParsingContext context) {
    context = context.section("hiddenBlocks");

    for (String value : section.getStringList("hiddenBlocks")) {
      this.hiddenBlocks.add(BlockParser.parseBlockOrBlockTag(registry, context, value, true));
    }

    if (this.hiddenBlocks.isEmpty()) {
      context.error(ConfigMessage.MISSING_OR_EMPTY);
    }
  }

  private void serializeHiddenBlocks(ConfigurationSection section) {
    List<String> blockNames = new ArrayList<>();

    for (ConfigBlockValue block : this.hiddenBlocks) {
      blockNames.add(block.value());
    }

    section.set("hiddenBlocks", blockNames);
  }

  @Override
  public boolean layerObfuscation() {
    return this.layerObfuscation;
  }

  @Override
  public Iterable<ConfigBlockValue> hiddenBlocks() {
    return this.hiddenBlocks;
  }
}
