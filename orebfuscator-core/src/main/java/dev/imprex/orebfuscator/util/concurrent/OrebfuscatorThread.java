package dev.imprex.orebfuscator.util.concurrent;

import dev.imprex.orebfuscator.logging.LogLevel;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.SplittableRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.random.RandomGenerator;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.logging.OfcLogger;

@NullMarked
public class OrebfuscatorThread extends Thread implements UncaughtExceptionHandler {

  private static final SplittableGenerator ROOT_GENERATOR = createRootRandom();
  private static final AtomicInteger THREAD_ID = new AtomicInteger();

  private static SplittableGenerator createRootRandom() {
    try {
      return SplittableGenerator.of("L64X128StarStarRandom");
    } catch (IllegalArgumentException e) {
      OfcLogger.warn("Couldn't create L64X128StarStarRandom using SplittableRandom as fallback");
      OfcLogger.debug(java.util.random.RandomGeneratorFactory.all()
          .map(RandomGeneratorFactory::name).sorted().collect(Collectors.joining(", ")));
      return new SplittableRandom();
    }
  }

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
