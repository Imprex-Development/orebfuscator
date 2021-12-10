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

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.obfuscation.ObfuscationResult;
import net.imprex.orebfuscator.util.ChunkPosition;

public class AsyncChunkSerializer implements Runnable {

	private final Lock lock = new ReentrantLock();
	private final Condition notFull = lock.newCondition();
	private final Condition notEmpty = lock.newCondition();

	private final Map<ChunkPosition, Runnable> tasks = new HashMap<>();
	private final Queue<ChunkPosition> positions = new LinkedList<>();
	private final int maxTaskQueueSize;

	private final Thread thread;
	private volatile boolean running = true;

	public AsyncChunkSerializer(Orebfuscator orebfuscator) {
		this.maxTaskQueueSize = orebfuscator.getOrebfuscatorConfig().cache().maximumTaskQueueSize();

		this.thread = new Thread(Orebfuscator.THREAD_GROUP, this, "ofc-chunk-serializer");
		this.thread.setDaemon(true);
		this.thread.start();
	}

	public CompletableFuture<ObfuscationResult> read(ChunkPosition position) {
		this.lock.lock();
		try {
			Runnable task = this.tasks.get(position);
			if (task instanceof WriteTask) {
				return CompletableFuture.completedFuture(((WriteTask) task).chunk);
			} else if (task instanceof ReadTask) {
				return ((ReadTask) task).future;
			} else {
				CompletableFuture<ObfuscationResult> future = new CompletableFuture<>();
				this.queueTask(position, new ReadTask(position, future));
				return future;
			}
		} finally {
			this.lock.unlock();
		}
	}

	public void write(ChunkPosition position, ObfuscationResult chunk) {
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
		while (this.positions.size() >= this.maxTaskQueueSize) {
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
		private final ObfuscationResult chunk;

		public WriteTask(ChunkPosition position, ObfuscationResult chunk) {
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
		private final CompletableFuture<ObfuscationResult> future;

		public ReadTask(ChunkPosition position, CompletableFuture<ObfuscationResult> future) {
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
