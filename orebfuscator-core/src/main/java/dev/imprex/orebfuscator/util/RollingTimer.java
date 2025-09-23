package dev.imprex.orebfuscator.util;

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
    
    private Instance()
    {}

    public void stop() {
      if (running) {
        add(System.nanoTime() - time);
        running = false;
      }
    }
  }
}
