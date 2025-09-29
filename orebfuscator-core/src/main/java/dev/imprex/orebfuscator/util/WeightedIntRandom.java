package dev.imprex.orebfuscator.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class WeightedIntRandom {

  public static Builder builder() {
    return new Builder();
  }

  /**
   * Sum of all weights
   */
  private final int weight;

  /**
   * Flat int array where each entry has a probability of one
   *
   * @implNote use flat int array instead of treemap cause O(N*log(N)) is too slow since this is the second hottest code
   * path
   */
  private final int[] entries;

  private WeightedIntRandom(Builder builder) {
    this.weight = builder.weight;
    this.entries = new int[builder.weight];

    int index = 0;
    for (Map.Entry<Integer, Integer> entry : builder.entries.entrySet()) {
      for (int weight = 0; weight < entry.getValue(); weight++) {
        this.entries[index++] = entry.getKey();
      }
    }
  }

  public int next() {
    int index = ThreadLocalRandom.current().nextInt(this.weight);
    return this.entries[index];
  }

  public static class Builder {

    private int weight = 0;

    private final Map<Integer, Integer> entries = new HashMap<>();

    private Builder() {
    }

    /**
     * Returns true if this random did not already contain the specified value
     */
    public boolean add(int value, int weight) {
      if (entries.putIfAbsent(value, weight) == null) {
        this.weight += weight;
        return true;
      }
      return false;
    }

    public WeightedIntRandom build() {
      return new WeightedIntRandom(this);
    }
  }
}
