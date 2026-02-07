package dev.imprex.orebfuscator.statistics;

import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import dev.imprex.orebfuscator.util.RollingAverage;
import dev.imprex.orebfuscator.util.RollingTimer;

public class ObfuscationStatistics implements StatisticsSource {

  public final RollingTimer debofuscation = new RollingTimer(4096);

  public final RollingTimer executorWaitTime = new RollingTimer(4096);
  public final RollingTimer executorUtilization = new RollingTimer(4096);

  public final RollingTimer proximityWait = new RollingTimer(4096);
  public final RollingTimer proximityProcess = new RollingTimer(4096);

  public final RollingAverage missingNeighboringChunks = new RollingAverage(4096);

  public final RollingAverage originalChunkSize = new RollingAverage(4096);
  public final RollingAverage obfuscatedChunkSize = new RollingAverage(4096);

  @Override
  public void add(StringJoiner joiner) {
    long debofuscation = (long) this.debofuscation.average();

    joiner.add(String.format(" - debofuscation: %s",
        time(debofuscation)));

    long executorWaitTime = (long) this.executorWaitTime.average();
    double executorUtilization = this.executorUtilization.average();

    joiner.add(String.format(" - executor (wait/utilization): %s / %s",
        time(executorWaitTime), percent(executorUtilization)));

    double proximityWait = this.proximityWait.average();
    double proximityProcess = this.proximityProcess.average();
    double proximityTotalTime = proximityWait + proximityProcess;

    double proximityUtilization = 0;
    if (proximityTotalTime > 0) {
      proximityUtilization = (double) proximityProcess / proximityTotalTime;
    }

    joiner.add(String.format(" - proximity utilization: %s",
        percent(proximityUtilization)));

    double missingNeighboringChunks = this.missingNeighboringChunks.average();

    joiner.add(String.format(" - missingNeighbors: %s",
        faction(missingNeighboringChunks)));

    long originalChunkSize = (long) this.originalChunkSize.average();
    long obfuscatedChunkSize = (long) this.obfuscatedChunkSize.average();

    double chunkSizeRatio = 1;
    if (originalChunkSize > 0) {
      chunkSizeRatio = (double) obfuscatedChunkSize / originalChunkSize;
    }

    joiner.add(String.format(" - chunk size (org/obf/rat): %s / %s / %s ",
        bytes(originalChunkSize), bytes(obfuscatedChunkSize), percent(chunkSizeRatio)));
  }

  @Override
  public void debug(BiConsumer<String, String> consumer) {
    consumer.accept("debofuscation", this.debofuscation.debugLong(this::time));

    consumer.accept("executorWaitTime", this.executorWaitTime.debugLong(this::time));
    consumer.accept("executorUtilization", this.executorUtilization.debugDouble(this::percent));

    consumer.accept("proximityWait", this.proximityWait.debugLong(this::time));
    consumer.accept("proximityProcess", this.proximityProcess.debugDouble(this::percent));

    consumer.accept("missingNeighboringChunks", this.missingNeighboringChunks.debugDouble(this::faction));

    consumer.accept("originalChunkSize", this.originalChunkSize.debugLong(this::bytes));
    consumer.accept("obfuscatedChunkSize", this.obfuscatedChunkSize.debugLong(this::bytes));
  }
}
