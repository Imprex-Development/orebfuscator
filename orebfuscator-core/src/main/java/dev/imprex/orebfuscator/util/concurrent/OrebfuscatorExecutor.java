package dev.imprex.orebfuscator.util.concurrent;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.statistics.OrebfuscatorStatistics;

// TODO: public schedule method
@NullMarked
public class OrebfuscatorExecutor implements Executor {

  private final ScheduledExecutorService scheduledExecutorService =
      Executors.newScheduledThreadPool(1, r -> new Thread(OrebfuscatorCore.THREAD_GROUP, r, "orebfuscator-scheduler"));

  private final int poolSize;
  private final ExecutorService executorService;

  private final LongAdder run = new LongAdder();
  private long updateTime = System.nanoTime();

  private final OrebfuscatorStatistics statistics;

  public OrebfuscatorExecutor(OrebfuscatorCore orebfuscator) {
    this.statistics = orebfuscator.statistics();

    this.poolSize = orebfuscator.config().advanced().obfuscationThreads();
    this.executorService = Executors.newFixedThreadPool(this.poolSize, OrebfuscatorThread::new);

    this.scheduledExecutorService.scheduleAtFixedRate(this::updateStatistics, 1L, 1L, TimeUnit.SECONDS);
  }

  @Override
  public void execute(Runnable command) {
    Objects.requireNonNull(command);

    if (Thread.currentThread() instanceof OrebfuscatorThread) {
      // don't time runnables if we're already on one of our threads as we can only enter
      // our thread through a timed runnable
      command.run();
    } else {
      executorService.execute(new TimedRunnable(command));
    }
  }

  private void updateStatistics() {
    long time = System.nanoTime();
    try {
      long run = this.run.sumThenReset();
      long available = this.poolSize * (time - this.updateTime);

      // invalid value don't know how to interpret it
      if (run < 0 || run > available) {
        return;
      }

      double utilization = available == 0 ? 0.0 : (double) run / available;
      statistics.obfuscation.executorUtilization.add(utilization);
    } finally {
      this.updateTime = time;
    }
  }

  private class TimedRunnable implements Runnable {

    private final long enqueuedAt = System.nanoTime();

    private final Runnable delegate;

    public TimedRunnable(Runnable delegate) {
      this.delegate = delegate;
    }

    @Override
    public void run() {
      long start = System.nanoTime();
      statistics.obfuscation.executorWaitTime.add(start - this.enqueuedAt);

      try {
        delegate.run();
      } finally {
        run.add(System.nanoTime() - start);
      }
    }
  }
}
