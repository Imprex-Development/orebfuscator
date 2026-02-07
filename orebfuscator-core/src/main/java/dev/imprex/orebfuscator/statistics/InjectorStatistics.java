package dev.imprex.orebfuscator.statistics;

import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import dev.imprex.orebfuscator.util.RollingAverage;
import dev.imprex.orebfuscator.util.RollingTimer;

public class InjectorStatistics implements StatisticsSource {

  public final RollingTimer pipelineDelayTotal = new RollingTimer(4096);
  public final RollingTimer pipelineDelayCache = new RollingTimer(4096);
  public final RollingTimer pipelineDelayNeighbors = new RollingTimer(4096);
  public final RollingTimer pipelineDelayProcessor = new RollingTimer(4096);

  public final RollingTimer injectorDelaySync = new RollingTimer(4096);
  public final RollingAverage injectorBatchSize = new RollingAverage(4096);

  public final RollingTimer packetDelayAny = new RollingTimer(4096);
  public final RollingTimer packetDelayChunk = new RollingTimer(4096);

  @Override
  public void add(StringJoiner joiner) {
    long pipeline = (long) this.pipelineDelayTotal.average();
    long cache = (long) this.pipelineDelayCache.average();
    long neighbors = (long) this.pipelineDelayNeighbors.average();
    long processor = (long) this.pipelineDelayProcessor.average();

    joiner.add(String.format(" - pipelineDelay (t/c/n/p): %s / %s / %s / %s",
        time(pipeline), time(cache), time(neighbors), time(processor)));

    long sync = (long) this.injectorDelaySync.average();
    double batchSize = this.injectorBatchSize.average();

    joiner.add(String.format(" - injector (syncDelay/batchSize): %s / %.2f",
        time(sync), batchSize));

    long any = (long) this.packetDelayAny.average();
    long chunk = (long) this.packetDelayChunk.average();

    joiner.add(String.format(" - packetDelay (any/chunk): %s / %s",
        time(any), time(chunk)));
  }

  @Override
  public void debug(BiConsumer<String, String> consumer) {
    consumer.accept("pipelineDelayTotal", this.pipelineDelayTotal.debugLong(this::time));
    consumer.accept("pipelineDelayCache", this.pipelineDelayCache.debugLong(this::time));
    consumer.accept("pipelineDelayNeighbors", this.pipelineDelayNeighbors.debugLong(this::time));
    consumer.accept("pipelineDelayProcessor", this.pipelineDelayProcessor.debugLong(this::time));

    consumer.accept("injectorDelaySync", this.injectorDelaySync.debugLong(this::time));
    consumer.accept("injectorBatchSize", this.injectorBatchSize.debugLong(this::time));

    consumer.accept("packetDelayAny", this.packetDelayAny.debugLong(this::time));
    consumer.accept("packetDelayChunk", this.packetDelayChunk.debugLong(this::time));
  }
}
