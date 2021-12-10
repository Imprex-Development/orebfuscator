package net.imprex.orebfuscator.obfuscation;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.bukkit.Bukkit;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.config.AdvancedConfig;

class ObfuscationTaskDispatcher implements Runnable {

	private final ObfuscationProcessor processor;

	private final Queue<ObfuscationRequest> requests = new ConcurrentLinkedQueue<>();
	private final Queue<ObfuscationTask> tasks = new ConcurrentLinkedQueue<>();

	private final long availableNanosPerTick;
	private final ObfuscationTaskWorker[] worker;

	public ObfuscationTaskDispatcher(Orebfuscator orebfuscator, ObfuscationProcessor processor) {
		this.processor = processor;

		AdvancedConfig config = orebfuscator.getOrebfuscatorConfig().advanced();
		this.availableNanosPerTick = TimeUnit.MILLISECONDS.toNanos(config.maxMillisecondPerTick());

		this.worker = new ObfuscationTaskWorker[config.obfuscationWorkerThreads()];
		for (int i = 0; i < this.worker.length; i++) {
			this.worker[i] = new ObfuscationTaskWorker(this, this.processor);
		}

		Bukkit.getScheduler().runTaskTimer(orebfuscator, this, 0, 1);
	}

	public void submitRequest(ObfuscationRequest request) {
		this.requests.offer(request);
	}

	public ObfuscationTask retrieveTask() throws InterruptedException {
		ObfuscationTask task;

		while ((task = this.tasks.poll()) == null) {
			LockSupport.park(this);
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
		}

		return task;
	}

	@Override
	public void run() {
		final long time = System.nanoTime();
		int tasksProduced = 0;

		ObfuscationRequest request = null;
		while (System.nanoTime() - time < this.availableNanosPerTick && (request = this.requests.poll()) != null) {
			this.tasks.offer(ObfuscationTask.fromRequest(request));
			tasksProduced++;
		}

		for (int i = 0; i < this.worker.length && tasksProduced > 0; i++) {
			if (this.worker[i].unpark()) {
				tasksProduced--;
			}
		}
	}

	public void shutdown() {
		for (ObfuscationTaskWorker worker : this.worker) {
			worker.shutdown();
		}
	}
}
