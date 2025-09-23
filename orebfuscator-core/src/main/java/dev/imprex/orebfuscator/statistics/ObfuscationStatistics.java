package dev.imprex.orebfuscator.statistics;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.LongSupplier;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.imprex.orebfuscator.util.RollingAverage;

public class ObfuscationStatistics implements StatisticsSource {

  private LongSupplier obfuscationQueueLength = () -> 0;

  public final RollingAverage originalChunkSize = new RollingAverage(4096);
  public final RollingAverage obfuscatedChunkSize = new RollingAverage(4096);

  public void setObfuscationQueueLength(LongSupplier supplier) {
    this.obfuscationQueueLength = Objects.requireNonNull(supplier);
  }

  @Override
  public void add(StringJoiner joiner) {
    long obfuscationQueueLength = this.obfuscationQueueLength.getAsLong();

    joiner.add(String.format(" - obfuscation (queue): %s", obfuscationQueueLength));

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
  public JsonElement json() {
    JsonObject object = new JsonObject();
    object.addProperty("obfuscationQueueLength", obfuscationQueueLength.getAsLong());
    object.addProperty("originalChunkSize", originalChunkSize.average());
    object.addProperty("obfuscatedChunkSize", obfuscatedChunkSize.average());
    return object;
  }
}
