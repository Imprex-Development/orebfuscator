package net.imprex.orebfuscator.obfuscation;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

import net.imprex.orebfuscator.Orebfuscator;

class ObfuscationTaskWorker implements Runnable {

	private static final AtomicInteger WORKER_ID = new AtomicInteger();

	private final ObfuscationTaskDispatcher dispatcher;
	private final ObfuscationTaskProcessor processor;

	private final Thread thread;
	private volatile boolean running = true;

	public ObfuscationTaskWorker(ObfuscationTaskDispatcher dispatcher, ObfuscationTaskProcessor processor) {
		this.dispatcher = dispatcher;
		this.processor = processor;

		this.thread = new Thread(Orebfuscator.THREAD_GROUP, this, "ofc-task-worker-" + WORKER_ID.getAndIncrement());
		this.thread.setDaemon(true);
		this.thread.start();
	}

	public boolean unpark() {
		if (LockSupport.getBlocker(this.thread) != null) {
			LockSupport.unpark(this.thread);
			return true;
		}
		return false;
	}

	@Override
	public void run() {
		while (this.running) {
			try {
				this.processor.process(this.dispatcher.retrieveTask());
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
