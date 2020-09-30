package net.imprex.orebfuscator.cache;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.imprex.orebfuscator.obfuscation.ObfuscatedChunk;
import net.imprex.orebfuscator.util.ChunkPosition;

public class AsyncChunkSerializer implements Runnable {

	private final Lock lock = new ReentrantLock();
	private final Condition condition = lock.newCondition();

	private final Map<ChunkPosition, Runnable> tasks = new HashMap<>();
	private final Queue<ChunkPosition> positions = new LinkedList<>();

	private final AtomicBoolean suspended = new AtomicBoolean();
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
		Runnable prevTask = this.tasks.put(position, nextTask);
		if (prevTask == null) {
			this.positions.offer(position);
		}

		if (this.suspended.compareAndSet(true, false)) {
			this.condition.signal();
		}

		return prevTask;
	}

	@Override
	public void run() {
		while (this.running || !this.positions.isEmpty()) {
			this.lock.lock();
			try {
				if (this.positions.isEmpty() && this.suspended.compareAndSet(false, true)) {
					this.condition.awaitUninterruptibly();
				} else {
					this.tasks.remove(this.positions.poll()).run();
				}
			} finally {
				this.lock.unlock();
			}
		}
	}

	public void close() {
		this.lock.lock();
		try {
			this.running = false;
			if (this.suspended.compareAndSet(true, false)) {
				this.condition.signal();
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
