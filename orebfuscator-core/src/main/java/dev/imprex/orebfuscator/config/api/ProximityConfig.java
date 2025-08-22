package dev.imprex.orebfuscator.config.api;

import java.util.Map;

import org.joml.Matrix4f;

import dev.imprex.orebfuscator.config.components.ConfigBlockValue;

public interface ProximityConfig extends WorldConfig {

  int distance();

  boolean frustumCullingEnabled();

  float frustumCullingMinDistanceSquared();

  Matrix4f frustumCullingProjectionMatrix();

  boolean rayCastCheckEnabled();

  boolean rayCastCheckOnlyCheckCenter();

  Iterable<Map.Entry<ConfigBlockValue, Integer>> hiddenBlocks();
}
