package dev.imprex.orebfuscator.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.DoubleFunction;
import java.util.function.LongFunction;
import com.google.common.util.concurrent.AtomicDouble;

// TODO: nullability
public class RollingAverage {

  private static final VarHandle BUFFER_HANDLE = MethodHandles.arrayElementVarHandle(double[].class);

  private final double[] buffer;

  private final AtomicInteger head = new AtomicInteger(0);
  private final AtomicInteger size = new AtomicInteger(0);

  private final AtomicDouble min = new AtomicDouble(Long.MAX_VALUE);
  private final AtomicDouble max = new AtomicDouble(Long.MIN_VALUE);

  public RollingAverage(int capacity) {
    this.buffer = new double[capacity];
  }

  public void add(double value) {
    int index = head.getAndUpdate(h -> (h + 1) % buffer.length);
    BUFFER_HANDLE.setRelease(buffer, index, value);

    if (size.get() < buffer.length) {
      size.updateAndGet(s -> s < buffer.length ? s + 1 : s);
    }

    if (size.get() >= buffer.length) {
      min.getAndUpdate(prev -> Math.min(prev, value));
      max.getAndUpdate(prev -> Math.max(prev, value));
    }
  }

  public double average() {
    int size = this.size.get();
    if (size == 0) {
      return 0;
    }

    double sum = 0;
    for (int i = 0; i < size; i++) {
      sum += (double) BUFFER_HANDLE.getAcquire(buffer, i);
    }

    return sum / size;
  }

  private double percentile(double p) {
    int size = this.size.get();
    if (size == 0) {
      return 0;
    }

    double[] copy = new double[size];
    for (int i = 0; i < size; i++) {
      copy[i] = (double) BUFFER_HANDLE.getAcquire(buffer, i);
    }

    Arrays.sort(copy);
    double rank = p * (size - 1);
    int lower = (int) Math.floor(rank);
    int upper = (int) Math.ceil(rank);
    if (lower == upper) {
      return copy[lower];
    }
    // Linear interpolation
    return copy[lower] + (copy[upper] - copy[lower]) * (rank - lower);
  }

  public String debugLong(LongFunction<String> formatter) {
    var n = size.get();
    var avg = formatter.apply((long) average());
    var p95 = formatter.apply((long) percentile(0.95));
    var p99 = formatter.apply((long) percentile(0.99));
    var min = formatter.apply((long) this.min.get());
    var max = formatter.apply((long) this.max.get());

    return String.format("{size=%d, avg=%s, p95=%s, p99=%s, min=%s, max=%s}", n, avg, p95,
        p99, min, max);
  }

  public String debugDouble(DoubleFunction<String> formatter) {
    var n = size.get();
    var avg = formatter.apply(average());
    var p95 = formatter.apply(percentile(0.95));
    var p99 = formatter.apply(percentile(0.99));
    var min = formatter.apply(this.min.get());
    var max = formatter.apply(this.max.get());

    return String.format("{size=%d, avg=%s, p95=%s, p99=%s, min=%s, max=%s}", n, avg, p95,
        p99, min, max);
  }
}
