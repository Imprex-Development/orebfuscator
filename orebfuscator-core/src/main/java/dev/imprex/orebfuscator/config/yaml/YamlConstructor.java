package dev.imprex.orebfuscator.config.yaml;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;

class YamlConstructor extends Constructor {

  public YamlConstructor(LoaderOptions loadingConfig) {
    super(loadingConfig);
  }

  @Override
  public void flattenMapping(MappingNode node) {
    super.flattenMapping(node);
  }

  @Override
  public Object constructObject(Node node) {
    return super.constructObject(node);
  }
}
