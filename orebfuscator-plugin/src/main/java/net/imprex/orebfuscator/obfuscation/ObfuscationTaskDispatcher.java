package net.imprex.orebfuscator.obfuscation;

import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import dev.imprex.orebfuscator.config.api.AdvancedConfig;
import net.imprex.orebfuscator.Orebfuscator;

class ObfuscationTaskDispatcher {

	private static final long DEFAULT_PARK_DURATION = TimeUnit.MILLISECONDS.toNanos(1);
	private static final long MAX_PARK_DURATION = TimeUnit.MILLISECONDS.toNanos(16);

	private final Queue<ObfuscationTask> tasks = new ConcurrentLinkedQueue<>();

	private final ObfuscationProcessor processor;
	private final ObfuscationTaskWorker[] worker;

	public ObfuscationTaskDispatcher(Orebfuscator orebfuscator, ObfuscationProcessor processor) {
		this.processor = processor;

		AdvancedConfig config = orebfuscator.getOrebfuscatorConfig().advanced();
		this.worker = new ObfuscationTaskWorker[config.obfuscationThreads()];
		for (int i = 0; i < this.worker.length; i++) {
			this.worker[i] = new ObfuscationTaskWorker(this, this.processor);
		}

		var statistics = orebfuscator.getStatistics();
		statistics.setObfuscationQueueLengthSupplier(() -> this.tasks.size());
		statistics.setObfuscationWaitTime(() -> (long) Arrays.stream(this.worker)
				.mapToDouble(ObfuscationTaskWorker::waitTime).average().orElse(0d));
		statistics.setObfuscationProcessTime(() -> (long) Arrays.stream(this.worker)
				.mapToDouble(ObfuscationTaskWorker::processTime).average().orElse(0d));
	}

	public void submitRequest(ObfuscationRequest request) {
		ObfuscationTask.fromRequest(request).whenComplete((task, throwable) -> {
			if (throwable != null) {
				request.completeExceptionally(throwable);
			} else {
				this.tasks.offer(task);
			}
		});
	}

	public ObfuscationTask retrieveTask() throws InterruptedException {
		ObfuscationTask task;

		long parkDuration = DEFAULT_PARK_DURATION;
		for (int i = 0; (task = this.tasks.poll()) == null; i++) {
			if (i < 8192) {
				Thread.onSpinWait();
			} else {
				LockSupport.parkNanos(this, parkDuration);

				// exponential backoff
				if (parkDuration < MAX_PARK_DURATION) {
					parkDuration *= 2;
				}
			}

			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
		}

		return task;
	}

	public void shutdown() {
		for (ObfuscationTaskWorker worker : this.worker) {
			worker.shutdown();
		}
	}
}
