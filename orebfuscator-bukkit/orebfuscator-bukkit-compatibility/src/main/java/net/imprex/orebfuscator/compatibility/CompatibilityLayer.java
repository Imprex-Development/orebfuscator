package net.imprex.orebfuscator.compatibility;

public interface CompatibilityLayer {

  boolean isGameThread();

  CompatibilityScheduler getScheduler();
}
