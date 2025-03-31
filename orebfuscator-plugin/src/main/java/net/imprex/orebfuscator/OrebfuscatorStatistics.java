package net.imprex.orebfuscator;

import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.DoubleSupplier;
import java.util.function.LongSupplier;

import com.google.gson.JsonObject;

public class OrebfuscatorStatistics {

    private static final long MICRO_SCALE  = 1000L;
    private static final long MILLI_SCALE  = 1000L * MICRO_SCALE;
    private static final long SECOND_SCALE = 1000L * MILLI_SCALE;

	private static String formatPrecent(double percent) {
		return String.format("%.2f%%", percent * 100);
	}

	private static String formatBytes(long bytes) {
		if (bytes > 1073741824L) {
			return String.format("%.1f GiB", bytes / 1073741824d);
		} else if (bytes > 1048576L) {
			return String.format("%.1f MiB", bytes / 1048576d);
		} else if (bytes > 1024L) {
			return String.format("%.1f KiB", bytes / 1024d);
		} else {
			return String.format("%d B", bytes);
		}
	}

	private static String formatNanos(double nanos) {
		if (nanos > SECOND_SCALE) {
			return String.format("%.2f s", nanos / SECOND_SCALE);
		} else if (nanos > MILLI_SCALE) {
			return String.format("%.2f ms", nanos / MILLI_SCALE);
		} else if (nanos > MICRO_SCALE) {
			return String.format("%.2f µs", nanos / MICRO_SCALE);
		} else {
			return String.format("%d ns", nanos);
		}
	}

	private final LongAdder cacheHitCountMemory = new LongAdder();
	private final LongAdder cacheHitCountDisk = new LongAdder();
	private final LongAdder cacheMissCount = new LongAdder();
	private final LongAdder cacheEstimatedSize = new LongAdder();
	private LongSupplier memoryCacheSize = () -> 0;
	private LongSupplier diskCacheQueueLength = () -> 0;
	private LongSupplier obfuscationQueueLength = () -> 0;
	private DoubleSupplier averagePacketDelay = () -> 0;

	public void onCacheHitMemory() {
		this.cacheHitCountMemory.increment();
	}

	public void onCacheHitDisk() {
		this.cacheHitCountDisk.increment();
	}

	public void onCacheMiss() {
		this.cacheMissCount.increment();
	}

	public void onCacheSizeChange(int delta) {
		this.cacheEstimatedSize.add(delta);
	}

	public void setMemoryCacheSizeSupplier(LongSupplier supplier) {
		this.memoryCacheSize = Objects.requireNonNull(supplier);
	}

	public void setDiskCacheQueueLengthSupplier(LongSupplier supplier) {
		this.diskCacheQueueLength = Objects.requireNonNull(supplier);
	}

	public void setObfuscationQueueLengthSupplier(LongSupplier supplier) {
		this.obfuscationQueueLength = Objects.requireNonNull(supplier);
	}

	public void setAveragePacketDelay(DoubleSupplier supplier) {
		this.averagePacketDelay = Objects.requireNonNull(supplier);
	}

	@Override
	public String toString() {
		long cacheHitCountMemory = this.cacheHitCountMemory.sum();
		long cacheHitCountDisk = this.cacheHitCountDisk.sum();
		long cacheMissCount = this.cacheMissCount.sum();
		long cacheEstimatedSize = this.cacheEstimatedSize.sum();
		long memoryCacheSize = this.memoryCacheSize.getAsLong();
		long diskCacheQueueLength = this.diskCacheQueueLength.getAsLong();
		long obfuscationQueueLength = this.obfuscationQueueLength.getAsLong();
		double averagePacketDelay = this.averagePacketDelay.getAsDouble();

		double totalCacheRequest = (double) (cacheHitCountMemory + cacheHitCountDisk + cacheMissCount);

		double memoryCacheHitRate = 0.0d;
		double diskCacheHitRate = 0.0d;
		if (totalCacheRequest > 0) {
			memoryCacheHitRate = (double) cacheHitCountMemory / totalCacheRequest;
			diskCacheHitRate = (double) cacheHitCountDisk / totalCacheRequest;
		}
		
		long memoryCacheBytesPerEntry = 0;
		if (memoryCacheSize > 0) {
			memoryCacheBytesPerEntry = cacheEstimatedSize / memoryCacheSize;
		}

		StringBuilder builder = new StringBuilder("Here are some useful statistics:\n");

		builder.append(" - memoryCacheHitRate: ").append(formatPrecent(memoryCacheHitRate)).append('\n');
		builder.append(" - diskCacheHitRate: ").append(formatPrecent(diskCacheHitRate)).append('\n');
		builder.append(" - memoryCacheEstimatedSize: ").append(formatBytes(cacheEstimatedSize)).append('\n');
		builder.append(" - memoryCacheBytesPerEntry: ").append(formatBytes(memoryCacheBytesPerEntry)).append('\n');
		builder.append(" - memoryCacheEntries: ").append(memoryCacheSize).append('\n');
		builder.append(" - diskCacheQueueLength: ").append(diskCacheQueueLength).append('\n');
		builder.append(" - obfuscationQueueLength: ").append(obfuscationQueueLength).append('\n');
		builder.append(" - averagePacketDelay: ").append(formatNanos(averagePacketDelay)).append('\n');

		return builder.toString();
	}

	public JsonObject toJson() {
		JsonObject object = new JsonObject();

		object.addProperty("cacheHitCountMemory", this.cacheHitCountMemory.sum());
		object.addProperty("cacheHitCountDisk", this.cacheHitCountDisk.sum());
		object.addProperty("cacheMissCount", this.cacheMissCount.sum());
		object.addProperty("cacheEstimatedSize", this.cacheEstimatedSize.sum());
		object.addProperty("memoryCacheSize", this.memoryCacheSize.getAsLong());
		object.addProperty("diskCacheQueueLength", this.diskCacheQueueLength.getAsLong());
		object.addProperty("obfuscationQueueLength", this.obfuscationQueueLength.getAsLong());
		object.addProperty("averagePacketDelayNano", this.averagePacketDelay.getAsDouble());

		return object;
	}
}
