package net.imprex.orebfuscator;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import com.google.gson.JsonObject;

public class OrebfuscatorStatistics {

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

	private final AtomicLong cacheHitCountMemory = new AtomicLong(0);
	private final AtomicLong cacheHitCountDisk = new AtomicLong(0);
	private final AtomicLong cacheMissCount = new AtomicLong(0);
	private final AtomicLong cacheEstimatedSize = new AtomicLong(0);
	private LongSupplier memoryCacheSize = () -> 0;
	private LongSupplier diskCacheQueueLength = () -> 0;
	private LongSupplier obfuscationQueueLength = () -> 0;

	public void onCacheHitMemory() {
		this.cacheHitCountMemory.incrementAndGet();
	}

	public void onCacheHitDisk() {
		this.cacheHitCountDisk.incrementAndGet();
	}

	public void onCacheMiss() {
		this.cacheMissCount.incrementAndGet();
	}

	public void onCacheSizeChange(int delta) {
		this.cacheEstimatedSize.addAndGet(delta);
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

	@Override
	public String toString() {
		long cacheHitCountMemory = this.cacheHitCountMemory.get();
		long cacheHitCountDisk = this.cacheHitCountDisk.get();
		long cacheMissCount = this.cacheMissCount.get();
		long cacheEstimatedSize = this.cacheEstimatedSize.get();
		long memoryCacheSize = this.memoryCacheSize.getAsLong();
		long diskCacheQueueLength = this.diskCacheQueueLength.getAsLong();
		long obfuscationQueueLength = this.obfuscationQueueLength.getAsLong();

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

		return builder.toString();
	}

	public JsonObject toJson() {
		JsonObject object = new JsonObject();

		object.addProperty("cacheHitCountMemory", this.cacheHitCountMemory.get());
		object.addProperty("cacheHitCountDisk", this.cacheHitCountDisk.get());
		object.addProperty("cacheMissCount", this.cacheMissCount.get());
		object.addProperty("cacheEstimatedSize", this.cacheEstimatedSize.get());
		object.addProperty("memoryCacheSize", this.memoryCacheSize.getAsLong());
		object.addProperty("diskCacheQueueLength", this.diskCacheQueueLength.getAsLong());
		object.addProperty("obfuscationQueueLength", this.obfuscationQueueLength.getAsLong());

		return object;
	}
}
