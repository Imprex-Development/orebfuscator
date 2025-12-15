package dev.imprex.orebfuscator.util.concurrent;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGenerator;
import java.util.random.RandomGenerator.SplittableGenerator;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.logging.OfcLogger;

// TODO: nullability
public class OrebfuscatorThread extends Thread implements UncaughtExceptionHandler {

  private static final SplittableGenerator ROOT_GENERATOR = SplittableGenerator.of("L64X128StarStarRandom");
  private static final AtomicInteger THREAD_ID = new AtomicInteger();

  private final RandomGenerator randomGenerator = ROOT_GENERATOR.split();

  public OrebfuscatorThread(Runnable target) {
    super(OrebfuscatorCore.THREAD_GROUP, target, "orebfuscator-thread-" + THREAD_ID.getAndIncrement());
    this.setUncaughtExceptionHandler(this);
  }

  public RandomGenerator random() {
    return this.randomGenerator;
  }

  @Override
  public void uncaughtException(Thread thread, Throwable throwable) {
    OfcLogger.error(String.format("Uncaught exception in: %s%n", thread.getName()), throwable);
  }
}
