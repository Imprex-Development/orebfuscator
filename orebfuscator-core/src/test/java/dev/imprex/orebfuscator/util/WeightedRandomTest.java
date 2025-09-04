package dev.imprex.orebfuscator.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

public class WeightedRandomTest {

  private static final long SEED = 1337;
  private static final int SAMPLES = 250_000;
  private static final double SIGMA = 2.0;

  private WeightedRandom createWeightedRandom(Map<Integer, Double> weights) {
    var builder = WeightedRandom.builder();
    for (var entry : weights.entrySet()) {
      builder.add(entry.getKey(), entry.getValue());
    }
    return builder.build();
  }

  private void assertDistribution(WeightedRandom weightedRandom, Map<Integer, Double> weights, int n, long seed,
      double sigma) {
    Random random = new Random(seed);

    Map<Integer, Integer> counts = new HashMap<>();
    for (int i = 0; i < n; i++) {
      counts.merge(weightedRandom.next(random), 1, Integer::sum);
    }

    double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();

    for (var entry : weights.entrySet()) {
      int key = entry.getKey();
      double prob = entry.getValue() / totalWeight;
      int count = counts.getOrDefault(key, 0);
      double observed = count / (double) n;

      // Binomial standard deviation for frequency
      double sd = Math.sqrt(prob * (1 - prob) / n);
      double tol = sigma * sd;

      assertTrue(Math.abs(observed - prob) <= tol, () -> String
          .format("freq for %d off: obs=%.6f exp=%.6f tol=%.6f (N=%d, sd=%.6g)", key, observed, prob, tol, n, sd));
    }
  }

  @Test
  public void testEmptyBuilderThrows() {
    var builder = WeightedRandom.builder();
    assertThrows(IllegalStateException.class, builder::build);
  }

  @Test
  public void testInvalidWeightsThrow() {
    var builder = WeightedRandom.builder();
    assertThrows(IllegalArgumentException.class, () -> builder.add(0, 0.0));
    assertThrows(IllegalArgumentException.class, () -> builder.add(0, -1.0));
    assertThrows(IllegalArgumentException.class, () -> builder.add(0, Double.POSITIVE_INFINITY));
    assertThrows(IllegalArgumentException.class, () -> builder.add(0, Double.NEGATIVE_INFINITY));
  }

  @Test
  public void testSingleEntry() {
    var weightedRandom = WeightedRandom.builder().add(42, 1.0).build();

    for (int i = 0; i < 1000; i++) {
      assertEquals(42, weightedRandom.next());
    }
  }

  @Test
  public void testSameSeeds() {
    var distribution = Map.of(1, 0.1, 2, 0.2, 3, 0.9);
    var weightedRandom = createWeightedRandom(distribution);

    Random randomA = new Random(42);
    Random randomB = new Random(42);

    List<Integer> sequenceA = new ArrayList<>();
    List<Integer> sequenceB = new ArrayList<>();
    for (int i = 0; i < 50; i++) {
      sequenceA.add(weightedRandom.next(randomA));
      sequenceB.add(weightedRandom.next(randomB));
    }

    assertEquals(sequenceA, sequenceB, "Sequences with same seed should match exactly");
  }

  @Test
  public void testUniformWeights() {
    var distribution = Map.of(1, 1.0, 2, 1.0, 3, 1.0, 4, 1.0);
    var weightedRandom = createWeightedRandom(distribution);
    assertDistribution(weightedRandom, distribution, SAMPLES, SEED, SIGMA);
  }

  @Test
  public void testRandomWeights() {
    var distribution = Map.of(1, 0.1, 2, 0.2, 3, 0.9);
    var weightedRandom = createWeightedRandom(distribution);
    assertDistribution(weightedRandom, distribution, SAMPLES, SEED, SIGMA);
  }

  @Test
  public void testMergedWeights() {
    var weightedRandom = WeightedRandom.builder()
        .add(1, 0.1)
        .add(2, 0.3)
        .add(1, 0.9)
        .build();

    var distribution = Map.of(1, 1.0, 2, 0.3);
    assertDistribution(weightedRandom, distribution, SAMPLES, SEED, SIGMA);
  }

  @Test
  public void testLargeWeights() {
    var distribution = Map.of(1, 0.1, 2, 0.2, 3, 100.0, 4, 0.5);
    var weightedRandom = createWeightedRandom(distribution);
    assertDistribution(weightedRandom, distribution, SAMPLES, SEED, SIGMA);
  }
}
