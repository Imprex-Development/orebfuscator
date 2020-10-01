package net.imprex.orebfuscator.cache;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.imprex.orebfuscator.obfuscation.ObfuscatedChunk;
import net.imprex.orebfuscator.util.ChunkPosition;

public class AsyncChunkSerializer implements Runnable {

	private static final int TASK_QUEUE_SIZE = 20_000; // roughly 200-400mb TODO add to config

	private final Lock lock = new ReentrantLock();
	private final Condition notFull = lock.newCondition();
	private final Condition notEmpty = lock.newCondition();

	private final Map<ChunkPosition, Runnable> tasks = new HashMap<>();
	private final Queue<ChunkPosition> positions = new LinkedList<>();

	private final Thread thread;
	private volatile boolean running = true;

	public AsyncChunkSerializer() {
		this.thread = new Thread(this, "chunk-serializer");
		this.thread.setDaemon(true);
		this.thread.start();
	}

	public CompletableFuture<ObfuscatedChunk> read(ChunkPosition position) {
		this.lock.lock();
		try {
			Runnable task = this.tasks.get(position);
			if (task instanceof WriteTask) {
				return CompletableFuture.completedFuture(((WriteTask) task).chunk);
			} else if (task instanceof ReadTask) {
				return ((ReadTask) task).future;
			} else {
				CompletableFuture<ObfuscatedChunk> future = new CompletableFuture<>();
				this.queueTask(position, new ReadTask(position, future));
				return future;
			}
		} finally {
			this.lock.unlock();
		}
	}

	public void write(ChunkPosition position, ObfuscatedChunk chunk) {
		this.lock.lock();
		try {
			Runnable prevTask = this.queueTask(position, new WriteTask(position, chunk));
			if (prevTask instanceof ReadTask) {
				((ReadTask) prevTask).future.complete(chunk);
			}
		} finally {
			this.lock.unlock();
		}
	}

	private Runnable queueTask(ChunkPosition position, Runnable nextTask) {
		while (this.positions.size() == TASK_QUEUE_SIZE) {
			this.notFull.awaitUninterruptibly();
		}

		if (!this.running) {
			throw new IllegalStateException("AsyncChunkSerializer already closed");
		}

		Runnable prevTask = this.tasks.put(position, nextTask);
		if (prevTask == null) {
			this.positions.offer(position);
		}

		this.notEmpty.signal();
		return prevTask;
	}

	@Override
	public void run() {
		while (this.running) {
			this.lock.lock();
			try {
				if (this.positions.isEmpty()) {
					this.notEmpty.await();
				}

				this.tasks.remove(this.positions.poll()).run();

				this.notFull.signal();
			} catch (InterruptedException e) {
				break;
			} finally {
				this.lock.unlock();
			}
		}
	}

	public void close() {
		this.lock.lock();
		try {
			this.running = false;
			this.thread.interrupt();

			while (!this.positions.isEmpty()) {
				Runnable task = this.tasks.remove(this.positions.poll());
				if (task instanceof WriteTask) {
					task.run();
				}
			}
		} finally {
			this.lock.unlock();
		}
	}

	private class WriteTask implements Runnable {
		private final ChunkPosition position;
		private final ObfuscatedChunk chunk;

		public WriteTask(ChunkPosition position, ObfuscatedChunk chunk) {
			this.position = position;
			this.chunk = chunk;
		}

		@Override
		public void run() {
			try {
				ChunkSerializer.write(position, chunk);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private class ReadTask implements Runnable {
		private final ChunkPosition position;
		private final CompletableFuture<ObfuscatedChunk> future;

		public ReadTask(ChunkPosition position, CompletableFuture<ObfuscatedChunk> future) {
			this.position = position;
			this.future = future;
		}

		@Override
		public void run() {
			try {
				future.complete(ChunkSerializer.read(position));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
