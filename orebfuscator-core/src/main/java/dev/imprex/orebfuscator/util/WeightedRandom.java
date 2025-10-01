package dev.imprex.orebfuscator.util;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import org.jetbrains.annotations.NotNull;

/**
 * Weighted random integer sampler using the
 * <a href="https://www.keithschwarz.com/darts-dice-coins/">alias method</a>.
 * <p>
 * This data structure allows efficient sampling of integer values according to arbitrary (positive) weights:
 * <ul>
 *   <li>Preprocessing/build time: {@code O(n)}</li>
 *   <li>Sampling time: {@code O(1)}</li>
 *   <li>Memory usage: {@code O(n)}</li>
 * </ul>
 * <p>
 * Usage example:
 * <pre>{@code
 * WeightedRandom wr = WeightedRandom.builder()
 *     .add(1, 0.5)   // value 1 with weight 0.5
 *     .add(2, 1.5)   // value 2 with weight 1.5
 *     .add(3, 3.0)   // value 3 with weight 3.0
 *     .build();
 *
 * int sample = wr.next(); // sample ~10% chance of 1, 30% of 2, 60% of 3
 * }</pre>
 *
 * <p>
 * This implementation is immutable and thread-safe for concurrent calls to
 * {@link #next()} after construction.
 */
public final class WeightedRandom {

  /**
   * Creates a new builder for constructing a {@link WeightedRandom}.
   *
   * @return a new {@link Builder} instance
   */
  public static Builder builder() {
    return new Builder();
  }

  private final int n;
  private final double totalWeight;
  private final boolean allWeightsEqual;

  private final int[] values;
  private final double[] probabilities;
  private final int[] alias;

  /**
   * Constructs a weighted random sampler from the given builder. Implements the alias method preprocessing algorithm.
   *
   * @param builder the builder containing values and weights
   */
  private WeightedRandom(@NotNull Builder builder) {
    double minWeight = Double.POSITIVE_INFINITY;
    double maxWeight = Double.NEGATIVE_INFINITY;
    double totalWeight = 0d;

    // implementation from: https://www.keithschwarz.com/darts-dice-coins/
    // STEP 1
    this.n = builder.entries.size();
    this.values = new int[n];
    this.probabilities = new double[n];
    this.alias = new int[n];

    // STEP 2
    double[] scaled = new double[n];
    Deque<Integer> small = new ArrayDeque<>();
    Deque<Integer> large = new ArrayDeque<>();

    // STEP 3
    int index = 0;
    for (var entry : builder.entries.entrySet()) {
      values[index] = entry.getKey();

      double weight = entry.getValue();
      minWeight = Math.min(minWeight, weight);
      maxWeight = Math.max(maxWeight, weight);
      totalWeight += weight;

      scaled[index++] = weight * n;
    }

    this.totalWeight = totalWeight;

    // treat near-equal weights as equal
    double span = maxWeight - minWeight;
    if (span <= Math.ulp(maxWeight) * 4 || span <= 1e-12 * Math.max(1.0, maxWeight)) {
      this.allWeightsEqual = true;
      return;
    }
    this.allWeightsEqual = false;

    // STEP 4
    for (int i = 0; i < n; i++) {
      if (scaled[i] < totalWeight) {
        small.addLast(i);
      } else {
        large.addLast(i);
      }
    }

    // STEP 5
    while (!small.isEmpty() && !large.isEmpty()) {
      int l = small.removeLast();
      int g = large.removeLast();
      probabilities[l] = scaled[l];
      alias[l] = g;
      scaled[g] = scaled[g] + scaled[l] - totalWeight;
      if (scaled[g] < totalWeight) {
        small.addLast(g);
      } else {
        large.addLast(g);
      }
    }

    // STEP 6
    while (!large.isEmpty()) {
      probabilities[large.removeLast()] = totalWeight;
    }

    // STEP 7
    while (!small.isEmpty()) {
      probabilities[small.removeLast()] = totalWeight;
    }
  }

  /**
   * Samples a random value using {@link ThreadLocalRandom}.
   *
   * @return a sampled integer according to the configured weights
   */
  public int next() {
    return next(ThreadLocalRandom.current());
  }

  /**
   * Samples a random value using the given random generator.
   *
   * @param random a {@link RandomGenerator} to use for randomness
   * @return a sampled integer according to the configured weights
   */
  public int next(@NotNull RandomGenerator random) {
    Objects.requireNonNull(random);

    int i = random.nextInt(this.n);
    if (this.allWeightsEqual) {
      return values[i];
    }

    int pick = random.nextDouble(totalWeight) < probabilities[i] ? i : alias[i];
    return values[pick];
  }

  /**
   * Builder for {@link WeightedRandom}.
   * <p>
   * Values are unique; adding the same value multiple times will merge and sum their weights.
   */
  public static class Builder {

    private final Map<Integer, Double> entries = new LinkedHashMap<>();

    private Builder() {
    }

    /**
     * Adds a value with the given weight to the distribution.
     * <p>
     * If the value already exists, its weight is increased by the given amount.
     *
     * @param value  the integer value to add
     * @param weight the weight associated with this value (must be positive and finite)
     * @return this builder for chaining
     * @throws IllegalArgumentException if {@code weight <= 0} or not finite
     */
    public Builder add(int value, double weight) {
      if (weight <= 0.0d || !Double.isFinite(weight)) {
        throw new IllegalArgumentException("Weight has to be greater zero and finite!");
      }

      this.entries.merge(value, weight, (a, b) -> a + b);
      return this;
    }

    /**
     * Builds a {@link WeightedRandom} from the current set of values and weights.
     *
     * @return a new {@link WeightedRandom} instance
     * @throws IllegalStateException if no values have been added
     */
    public WeightedRandom build() {
      if (this.entries.isEmpty()) {
        throw new IllegalStateException("No entries added!");
      }

      return new WeightedRandom(this);
    }
  }
}
