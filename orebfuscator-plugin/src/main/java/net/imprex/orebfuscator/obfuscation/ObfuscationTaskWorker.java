package net.imprex.orebfuscator.obfuscation;

import java.util.concurrent.atomic.AtomicInteger;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.util.RingTimer;

class ObfuscationTaskWorker implements Runnable {

	private static final AtomicInteger WORKER_ID = new AtomicInteger();

	private final ObfuscationTaskDispatcher dispatcher;
	private final ObfuscationProcessor processor;

	private final Thread thread;
	private volatile boolean running = true;

	private final RingTimer waitTimer = new RingTimer(100);
	private final RingTimer processTimer = new RingTimer(100);

	public ObfuscationTaskWorker(ObfuscationTaskDispatcher dispatcher, ObfuscationProcessor processor) {
		this.dispatcher = dispatcher;
		this.processor = processor;

		this.thread = new Thread(Orebfuscator.THREAD_GROUP, this, "ofc-task-worker-" + WORKER_ID.getAndIncrement());
		this.thread.setDaemon(true);
		this.thread.start();
	}

	public double getWaitTime() {
		return waitTimer.average();
	}

	public double getProcessTime() {
		return processTimer.average();
	}

	@Override
	public void run() {
		while (this.running) {
			try {
				ObfuscationTask task;

				waitTimer.start();
				try {
					task = dispatcher.retrieveTask();
				} finally {
					waitTimer.stop();
				}

				processTimer.start();
				try {
					processor.process(task);
				} finally {
					processTimer.stop();
				}
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	public void shutdown() {
		this.running = false;
		this.thread.interrupt();
	}
}
