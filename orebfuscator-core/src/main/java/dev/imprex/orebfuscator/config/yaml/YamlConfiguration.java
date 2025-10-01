/*
 * This code is adapted from the Bukkit/Spigot project:
 * https://hub.spigotmc.org/stash/projects/SPIGOT/repos/bukkit/browse/src/main/java/org/bukkit/
 * configuration/file/YamlConfiguration.java Copyright (C) 2011-2024 Bukkit Project (original
 * authors and contributors) Licensed under the GNU General Public License v3.0 (GPLv3)
 */
package dev.imprex.orebfuscator.config.yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.AnchorNode;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;

public class YamlConfiguration extends ConfigurationSection {

  @NotNull
  public static YamlConfiguration loadConfig(@NotNull Path path) throws IOException, InvalidConfigurationException {
    Objects.requireNonNull(path, "Path cannot be null");

    try (InputStream inputStream = Files.newInputStream(path)) {
      return loadConfig(inputStream);
    }
  }

  @NotNull
  public static YamlConfiguration loadConfig(@NotNull InputStream inputStream)
      throws IOException, InvalidConfigurationException {
    Objects.requireNonNull(inputStream, "InputStream cannot be null");

    try (Reader reader = new UnicodeReader(inputStream)) {
      YamlConfiguration configuration = new YamlConfiguration();
      configuration.load(reader);
      return configuration;
    } catch (YAMLException e) {
      throw new InvalidConfigurationException(e);
    }
  }

  private final DumperOptions dumperOptions = new DumperOptions();
  private final Representer representer = new Representer(dumperOptions);

  private final LoaderOptions loaderOptions = new LoaderOptions();
  private final YamlConstructor constructor = new YamlConstructor(loaderOptions);

  private final Yaml yaml = new Yaml(constructor, representer, dumperOptions, loaderOptions);
  private NodeCommentData commentData = new NodeCommentData();

  protected YamlConfiguration() {
    super("");

    dumperOptions.setIndent(2);
    dumperOptions.setWidth(80);
    dumperOptions.setProcessComments(true);
    dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    representer.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

    loaderOptions.setProcessComments(true);
  }

  public void clear() {
    this.map.clear();
  }

  public void save(@NotNull Path path) throws IOException {
    Objects.requireNonNull(path, "Path cannot be null");

    Files.createDirectories(path.getParent());

    try (Writer writer = Files.newBufferedWriter(path)) {
      dumperOptions.setProcessComments(true);
      save(writer);
    }
  }

  public String withoutComments() throws IOException {
    try (Writer writer = new StringWriter()) {
      dumperOptions.setProcessComments(false);
      save(writer);
      return writer.toString();
    }
  }

  private void save(@NotNull Writer writer) throws IOException {
    Objects.requireNonNull(writer, "Writer cannot be null");

    MappingNode node = toNodeTree(this.commentData, this);
    this.commentData.apply(node);

    if (isNullOrEmpty(node.getBlockComments()) && isNullOrEmpty(node.getEndComments()) && node.getValue().isEmpty()) {
      writer.write("");
    } else {
      if (node.getValue().isEmpty()) {
        node.setFlowStyle(DumperOptions.FlowStyle.FLOW);
      }
      yaml.serialize(node, writer);
    }
  }

  private void load(@NotNull Reader reader) throws YAMLException, InvalidConfigurationException {
    Objects.requireNonNull(reader, "Reader cannot be null");

    Node rawNode = yaml.compose(reader);
    if (rawNode != null && !(rawNode instanceof MappingNode)) {
      throw new InvalidConfigurationException("Top level is not a Map.");
    }

    this.map.clear();

    MappingNode node = (MappingNode) rawNode;
    if (node != null) {
      this.commentData = new NodeCommentData(node);
      fromNodeTree(node, this.commentData, this);
    }
  }

  private MappingNode toNodeTree(@Nullable NodeCommentData commentData, @NotNull ConfigurationSection section) {
    List<NodeTuple> nodeTuples = new ArrayList<>();

    for (Map.Entry<String, Object> entry : section.map.entrySet()) {
      Node key = representer.represent(entry.getKey());

      NodeCommentData childCommentData = null;
      if (commentData != null) {
        childCommentData = commentData.getChild(entry.getKey());
      }

      Node value;
      if (entry.getValue() instanceof ConfigurationSection configurationSection) {
        value = toNodeTree(childCommentData, configurationSection);
      } else {
        value = representer.represent(entry.getValue());
      }

      NodeTuple nodeTuple = new NodeTuple(key, value);
      if (childCommentData != null) {
        childCommentData.apply(nodeTuple);
      }

      nodeTuples.add(nodeTuple);
    }

    return new MappingNode(Tag.MAP, nodeTuples, DumperOptions.FlowStyle.BLOCK);
  }

  private void fromNodeTree(@NotNull MappingNode input, @NotNull NodeCommentData commentData,
      @NotNull ConfigurationSection section) throws InvalidConfigurationException {
    constructor.flattenMapping(input);

    for (NodeTuple nodeTuple : input.getValue()) {
      Node keyNode = nodeTuple.getKeyNode();
      String key = String.valueOf(constructor.constructObject(keyNode));
      if (key.isBlank() || key.indexOf(ConfigurationSection.PATH_SEPARATOR) != -1) {
        throw new InvalidConfigurationException(String.format("Invalid key%s%n  key can't be blank or contain any '%s'",
            keyNode.getStartMark(), ConfigurationSection.PATH_SEPARATOR));
      }

      Node valueNode = nodeTuple.getValueNode();
      while (valueNode instanceof AnchorNode value) {
        valueNode = value.getRealNode();
      }

      NodeCommentData childCommentData = new NodeCommentData(nodeTuple);
      commentData.addChild(key, childCommentData);

      if (valueNode instanceof MappingNode value) {
        fromNodeTree(value, childCommentData, section.createSection(key));
      } else {
        section.set(key, constructor.constructObject(valueNode));
      }
    }
  }

  private boolean isNullOrEmpty(Collection<?> collection) {
    return collection == null || collection.isEmpty();
  }
}
