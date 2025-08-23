package dev.imprex.orebfuscator.config.yaml;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.comments.CommentLine;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.SequenceNode;

class NodeCommentData {

  private final List<CommentLine> blockComments;
  private final List<CommentLine> inLineComments;
  private final List<CommentLine> endComments;

  private final Map<String, NodeCommentData> children = new HashMap<>();
  private final Map<Object, List<CommentLine>> sequenceValueComments;

  public NodeCommentData() {
    this.blockComments = Collections.emptyList();
    this.inLineComments = Collections.emptyList();
    this.endComments = null;
    this.sequenceValueComments = null;
  }

  public NodeCommentData(@NotNull NodeTuple nodeTuple) {
    Node keyNode = nodeTuple.getKeyNode();
    Node valueNode = nodeTuple.getValueNode();

    this.blockComments = keyNode.getBlockComments();
    this.endComments = null; // only present on root node

    if (valueNode instanceof SequenceNode) {
      // disallow inline comments for sequence nodes as they change indentation on dump
      this.inLineComments = Collections.emptyList();
    } else if (valueNode instanceof MappingNode) {
      this.inLineComments = keyNode.getInLineComments();
    } else {
      this.inLineComments = valueNode.getInLineComments();
    }

    // each value node in a sequence has its own comments so we need to store them
    if (valueNode instanceof SequenceNode sequenceNode) {
      this.sequenceValueComments = new HashMap<>();
      for (Node value : sequenceNode.getValue()) {
        if (value instanceof ScalarNode scalarNode) {
          // only store inline comments as the reset mess up indentation
          this.sequenceValueComments.put(scalarNode.getValue(), scalarNode.getInLineComments());
        }
      }
    } else {
      this.sequenceValueComments = null;
    }
  }

  public NodeCommentData(@NotNull MappingNode node) {
    this.blockComments = node.getBlockComments();
    this.inLineComments = node.getInLineComments();
    this.endComments = node.getEndComments();
    this.sequenceValueComments = null;
  }

  public void addChild(@NotNull String key, @NotNull NodeCommentData commentData) {
    this.children.put(key, commentData);
  }

  @Nullable
  public NodeCommentData getChild(@NotNull String key) {
    return this.children.get(key);
  }

  public void apply(@NotNull NodeTuple nodeTuple) {
    Node keyNode = nodeTuple.getKeyNode();
    Node valueNode = nodeTuple.getValueNode();

    keyNode.setBlockComments(this.blockComments);

    if (valueNode instanceof MappingNode) {
      keyNode.setInLineComments(this.inLineComments);
    } else if (!(valueNode instanceof SequenceNode)) {
      valueNode.setInLineComments(this.inLineComments);
    }

    // apply inline comments to each value node individually
    if (valueNode instanceof SequenceNode sequenceNode && this.sequenceValueComments != null) {
      for (Node value : sequenceNode.getValue()) {
        if (!(value instanceof ScalarNode scalarNode)) {
          continue;
        }

        List<CommentLine> inlineComments = this.sequenceValueComments.get(scalarNode.getValue());
        if (inlineComments != null) {
          scalarNode.setInLineComments(inlineComments);
        }
      }
    }
  }

  public void apply(@NotNull MappingNode node) {
    node.setBlockComments(this.blockComments);
    node.setInLineComments(this.inLineComments);
    node.setEndComments(this.endComments);
  }
}
