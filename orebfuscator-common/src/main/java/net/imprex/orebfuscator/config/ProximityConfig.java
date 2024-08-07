package net.imprex.orebfuscator.config;

import java.util.Map;

import org.joml.Matrix4f;

import net.imprex.orebfuscator.util.BlockProperties;

public interface ProximityConfig extends WorldConfig {

	int distance();

	boolean frustumCullingEnabled();

	float frustumCullingMinDistanceSquared();

	Matrix4f frustumCullingProjectionMatrix();

	boolean rayCastCheckEnabled();

	boolean rayCastCheckOnlyCheckCenter();

	Iterable<Map.Entry<BlockProperties, Integer>> hiddenBlocks();
}
