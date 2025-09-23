package dev.imprex.orebfuscator.statistics;

import java.util.StringJoiner;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.imprex.orebfuscator.util.RollingTimer;

public class InjectorStatistics implements StatisticsSource {

  public final RollingTimer packetDelayNeighbors = new RollingTimer(4096);
  public final RollingTimer packetDelayExecutor = new RollingTimer(4096);
  public final RollingTimer packetDelaySync = new RollingTimer(4096);
  public final RollingTimer packetDelayAsync = new RollingTimer(4096);
  public final RollingTimer packetDelayProcessor = new RollingTimer(4096);

  public final RollingTimer packetDelayTotal = new RollingTimer(4096);
  public final RollingTimer packetDelayWrite = new RollingTimer(4096);
  public final RollingTimer packetDelayFlush = new RollingTimer(4096);
  public final RollingTimer packetDelayWait = new RollingTimer(4096);

  @Override
  public void add(StringJoiner joiner) {
    long neighbors = (long) this.packetDelayNeighbors.average();
    long executor = (long) this.packetDelayExecutor.average();
    long async = (long) this.packetDelayAsync.average();
    long sync = (long) this.packetDelaySync.average();
    long processor = (long) this.packetDelayProcessor.average();

    joiner.add(String.format(" - chunkPacketDelay (n/e/a/s/p): %s / %s / %s / %s / %s",
        time(neighbors), time(executor), time(async), time(sync), time(processor)));
 
    long total = (long) this.packetDelayTotal.average();
    long write = (long) this.packetDelayWrite.average();
    long flush = (long) this.packetDelayFlush.average();
    long wait = (long) this.packetDelayWait.average();

    joiner.add(String.format(" - packetDelay (tot/wr/fl/wt): %s / %s / %s / %s",
        time(total), time(write), time(flush), time(wait)));
  }

  @Override
  public JsonElement json() {
    JsonObject object = new JsonObject();
    object.addProperty("packetDelayTotal", packetDelayTotal.average());
    object.addProperty("packetDelaySync", packetDelaySync.average());
    object.addProperty("packetDelayWrite", packetDelayWrite.average());
    object.addProperty("packetDelayFlush", packetDelayFlush.average());
    object.addProperty("packetDelayProcessor", packetDelayProcessor.average());
    return object;
  }
}
