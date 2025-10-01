package net.imprex.orebfuscator.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicInteger;

public class RollingAverage {

  private static final VarHandle BUFFER_HANDLE = MethodHandles.arrayElementVarHandle(long[].class);

  private final long[] buffer;

  private final AtomicInteger head = new AtomicInteger(0);
  private final AtomicInteger size = new AtomicInteger(0);

  public RollingAverage(int capacity) {
    this.buffer = new long[capacity];
  }

  public void add(long value) {
    int index = head.getAndUpdate(h -> (h + 1) % buffer.length);
    BUFFER_HANDLE.setRelease(buffer, index, value);

    if (size.get() < buffer.length) {
      size.updateAndGet(s -> s < buffer.length ? s + 1 : s);
    }
  }

  public double average() {
    int size = this.size.get();
    if (size == 0) {
      return 0;
    }

    double sum = 0;
    for (int i = 0; i < size; i++) {
      sum += (long) BUFFER_HANDLE.getAcquire(buffer, i);
    }

    return sum / size;
  }
}
