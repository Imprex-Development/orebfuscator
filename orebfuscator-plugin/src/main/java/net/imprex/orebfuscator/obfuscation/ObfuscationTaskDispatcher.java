package net.imprex.orebfuscator.obfuscation;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.LockSupport;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.config.AdvancedConfig;

class ObfuscationTaskDispatcher {

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

		orebfuscator.getStatistics().setObfuscationQueueLengthSupplier(() -> this.tasks.size());
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

		while ((task = this.tasks.poll()) == null) {
			// sleep for 1 tick = 50ms
			LockSupport.parkNanos(this, 50000000L);
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
