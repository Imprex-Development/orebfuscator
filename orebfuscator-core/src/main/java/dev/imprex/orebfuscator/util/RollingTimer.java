package dev.imprex.orebfuscator.util;

import java.util.concurrent.CompletionStage;

// TODO: nullability
public class RollingTimer extends RollingAverage {

  public RollingTimer(int capacity) {
    super(capacity);
  }

  public Instance start() {
    return new Instance();
  }

  public class Instance {

    private final long time = System.nanoTime();
    private boolean running = true;

    private Instance() {
    }

    public <T> CompletionStage<T> wrap(CompletionStage<T> completionStage) {
      return completionStage.whenComplete((a, b) -> stop());
    }

    public void stop() {
      if (running) {
        add(System.nanoTime() - time);
        running = false;
      }
    }
  }
}
