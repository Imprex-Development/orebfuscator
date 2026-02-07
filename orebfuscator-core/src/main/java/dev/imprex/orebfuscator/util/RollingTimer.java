package dev.imprex.orebfuscator.util;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class RollingTimer extends RollingAverage {

  public RollingTimer(int capacity) {
    super(capacity);
  }

  public Instance start() {
    return new Instance();
  }

  public class Instance {

    private final long time = System.nanoTime();
    private final AtomicBoolean running = new AtomicBoolean(true);

    private Instance() {
    }

    public <T> CompletionStage<T> wrap(CompletionStage<T> completionStage) {
      return completionStage.whenComplete((a, b) -> stop());
    }

    public void stop() {
      if (this.running.compareAndSet(true, false)) {
        add(System.nanoTime() - time);
      }
    }
  }
}
