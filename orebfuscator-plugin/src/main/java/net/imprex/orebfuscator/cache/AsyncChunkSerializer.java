package net.imprex.orebfuscator.cache;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.obfuscation.ObfuscatedChunk;
import net.imprex.orebfuscator.util.ChunkPosition;

public class AsyncChunkSerializer implements Runnable {

	private final Lock lock = new ReentrantLock();
	private final Condition notFull = lock.newCondition();
	private final Condition notEmpty = lock.newCondition();

	private final Map<ChunkPosition, SerializerTask> tasks = new HashMap<>();
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

	public CompletableFuture<ObfuscatedChunk> read(ChunkPosition position) {
		this.lock.lock();
		try {
			SerializerTask task = this.tasks.get(position);
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
			SerializerTask prevTask = this.queueTask(position, new WriteTask(position, chunk));
			if (prevTask instanceof ReadTask) {
				((ReadTask) prevTask).future.complete(chunk);
			}
		} finally {
			this.lock.unlock();
		}
	}

	private SerializerTask queueTask(ChunkPosition position, SerializerTask nextTask) {
		while (this.positions.size() >= this.maxTaskQueueSize) {
			// skip read task if queue is full, live obfuscation is cheaper
			if (nextTask instanceof ReadTask) {
				((ReadTask) nextTask).completeEmpty();
				return null;
			}
			this.notFull.awaitUninterruptibly();
		}

		if (!this.running) {
			throw new IllegalStateException("AsyncChunkSerializer already closed");
		}

		SerializerTask prevTask = this.tasks.put(position, nextTask);
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
				SerializerTask task = this.tasks.remove(this.positions.poll());
				if (task instanceof WriteTask) {
					task.run();
				}
			}
		} finally {
			this.lock.unlock();
		}
	}
}
