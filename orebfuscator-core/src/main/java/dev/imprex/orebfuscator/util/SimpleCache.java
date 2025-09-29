package dev.imprex.orebfuscator.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Simple cache implementation that removes the oldest element once a certain size is reached
 */
public class SimpleCache<K, V> extends LinkedHashMap<K, V> {

  private static final long serialVersionUID = -2732738355560313649L;

  private final int maximumSize;
  private final Consumer<Map.Entry<K, V>> remove;

  public SimpleCache(int maximumSize, Consumer<Map.Entry<K, V>> remove) {
    super(16, 0.75f, true);

    this.maximumSize = maximumSize;
    this.remove = remove;
  }

  @Override
  protected boolean removeEldestEntry(Map.Entry<K, V> entry) {
    if (this.size() > this.maximumSize) {
      this.remove.accept(entry);
      return true;
    }
    return false;
  }
}
