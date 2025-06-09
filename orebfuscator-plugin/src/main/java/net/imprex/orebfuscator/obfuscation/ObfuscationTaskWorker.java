package net.imprex.orebfuscator.obfuscation;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import net.imprex.orebfuscator.Orebfuscator;

class ObfuscationTaskWorker implements Runnable {

	private static final AtomicInteger WORKER_ID = new AtomicInteger();

	private final ObfuscationTaskDispatcher dispatcher;
	private final ObfuscationProcessor processor;

	private final Thread thread;
	private volatile boolean running = true;

	private final Queue<Long> lastWaitTime = new LinkedList<Long>();
	private final Queue<Long> lastProcessTime = new LinkedList<Long>();

	public ObfuscationTaskWorker(ObfuscationTaskDispatcher dispatcher, ObfuscationProcessor processor) {
		this.dispatcher = dispatcher;
		this.processor = processor;

		this.thread = new Thread(Orebfuscator.THREAD_GROUP, this, "ofc-task-worker-" + WORKER_ID.getAndIncrement());
		this.thread.setDaemon(true);
		this.thread.start();
	}

	public double getWaitTime() {
		return lastWaitTime.stream().mapToLong(Long::longValue).average().orElse(0d);
	}

	public double getProcessTime() {
		return lastProcessTime.stream().mapToLong(Long::longValue).average().orElse(0d);
	}

	@Override
	public void run() {
		while (this.running) {
			try {
				long waitStart = System.nanoTime();
				ObfuscationTask task = this.dispatcher.retrieveTask();
				long waitTime = System.nanoTime() - waitStart;

				// measure wait time
				while (lastWaitTime.size() >= 100) {
					lastWaitTime.poll();
				}
				lastWaitTime.offer(waitTime);
				
				long processStart = System.nanoTime();
				this.processor.process(task);
				long processTime = System.nanoTime() - processStart;

				// measure process time
				while (lastProcessTime.size() >= 100) {
					lastProcessTime.poll();
				}
				lastProcessTime.offer(processTime);
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
