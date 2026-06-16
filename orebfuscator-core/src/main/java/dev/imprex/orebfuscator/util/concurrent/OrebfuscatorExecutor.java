package dev.imprex.orebfuscator.util.concurrent;

import dev.imprex.orebfuscator.config.yaml.ConfigurationSection;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.statistics.OrebfuscatorStatistics;

@NullMarked
public class OrebfuscatorExecutor implements Executor {

  private final ScheduledThreadPoolExecutor scheduledExecutorService = new ScheduledThreadPoolExecutor(
      1, r -> new Thread(OrebfuscatorCore.THREAD_GROUP, r, "orebfuscator-scheduler"));

  private final int poolSize;
  private final ThreadPoolExecutor executorService;

  private final LongAdder run = new LongAdder();
  private long updateTime = System.nanoTime();

  private final OrebfuscatorStatistics statistics;

  public OrebfuscatorExecutor(OrebfuscatorCore orebfuscator) {
    this.statistics = orebfuscator.statistics();

    this.poolSize = orebfuscator.config().advanced().threads();
    this.executorService = new ThreadPoolExecutor(
        this.poolSize, this.poolSize,
        0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(),
        OrebfuscatorThread::new);

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

  public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
    return scheduledExecutorService.schedule(command, delay, unit);
  }

  public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
    return scheduledExecutorService.scheduleAtFixedRate(command, initialDelay, period, unit);
  }

  public boolean isShutdown() {
    return this.scheduledExecutorService.isShutdown() || this.executorService.isShutdown();
  }

  public void shutdown() {
    this.scheduledExecutorService.shutdownNow();
    this.executorService.shutdownNow();
  }

  public void dump(ConfigurationSection section) {
    dumpExecutor(section.createSection("scheduler"), this.scheduledExecutorService);
    dumpExecutor(section.createSection("core"), this.executorService);

    ConfigurationSection threads = section.createSection("threads");

    for (var entry : Thread.getAllStackTraces().entrySet()) {
      Thread thread = entry.getKey();

      if (!(thread instanceof OrebfuscatorThread)) {
        continue;
      }

      StackTraceElement[] stackTrace = entry.getValue();

      ConfigurationSection threadSection =
          threads.createSection("thread-" + thread.getId());

      threadSection.set("name", thread.getName());
      threadSection.set("id", thread.getId());
      threadSection.set("state", thread.getState().name());
      threadSection.set("alive", thread.isAlive());
      threadSection.set("daemon", thread.isDaemon());
      threadSection.set("priority", thread.getPriority());
      threadSection.set("stack", formatStackTrace(stackTrace));
    }
  }

  private static void dumpExecutor(ConfigurationSection section, ThreadPoolExecutor executor) {
    section.set("shutdown", executor.isShutdown());
    section.set("terminating", executor.isTerminating());
    section.set("terminated", executor.isTerminated());

    section.set("pool.size", executor.getPoolSize());
    section.set("pool.coreSize", executor.getCorePoolSize());
    section.set("pool.maxSize", executor.getMaximumPoolSize());
    section.set("pool.largestSize", executor.getLargestPoolSize());
    section.set("pool.active", executor.getActiveCount());

    long taskCount = executor.getTaskCount();
    long completedTaskCount = executor.getCompletedTaskCount();

    section.set("tasks.total", taskCount);
    section.set("tasks.completed", completedTaskCount);
    section.set("tasks.pending", Math.max(0, taskCount - completedTaskCount));

    BlockingQueue<Runnable> queue = executor.getQueue();
    section.set("queue.type", queue.getClass().getName());
    section.set("queue.size", queue.size());
    section.set("queue.remainingCapacity", queue.remainingCapacity());

    section.set("keepAliveMillis", executor.getKeepAliveTime(TimeUnit.MILLISECONDS));
    section.set("allowsCoreThreadTimeOut", executor.allowsCoreThreadTimeOut());

    section.set("threadFactory", executor.getThreadFactory().getClass().getName());
    section.set("rejectedExecutionHandler", executor.getRejectedExecutionHandler().getClass().getName());

    if (executor instanceof ScheduledThreadPoolExecutor scheduled) {
      section.set("scheduled.continueExistingPeriodicTasksAfterShutdown",
          scheduled.getContinueExistingPeriodicTasksAfterShutdownPolicy());
      section.set("scheduled.executeExistingDelayedTasksAfterShutdown",
          scheduled.getExecuteExistingDelayedTasksAfterShutdownPolicy());
      section.set("scheduled.removeOnCancel",
          scheduled.getRemoveOnCancelPolicy());
    }
  }

  private static List<String> formatStackTrace(StackTraceElement[] stackTrace) {
    return Arrays.stream(stackTrace)
        .map(element -> "at " + element)
        .collect(Collectors.toList());
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
