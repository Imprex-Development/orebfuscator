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

public class ChunkSerializer implements Runnable {

	private final Lock lock = new ReentrantLock();
	private final Condition condition = lock.newCondition();

	private final Map<ChunkPosition, Runnable> tasks = new HashMap<>();
	private final Queue<ChunkPosition> positions = new LinkedList<>();

	private final AtomicBoolean suspended = new AtomicBoolean();
	private final Thread thread;

	private volatile boolean running = true;

	public ChunkSerializer() {
		this.thread = new Thread(this, "chunk-serializer");
		this.thread.setDaemon(true);
		this.thread.start();
	}

	public CompletableFuture<ObfuscatedChunk> read(ChunkPosition position) {
		CompletableFuture<ObfuscatedChunk> future = new CompletableFuture<>();
		this.queueTask(position, () -> {
			try {
				future.complete(ChunkCacheSerializer.read(position));
			} catch (IOException e) {
				e.printStackTrace();
				future.complete(null);
			}
		});
		return future;
	}

	public void write(ChunkPosition position, ObfuscatedChunk chunk) {
		this.queueTask(position, () -> {
			try {
				ChunkCacheSerializer.write(position, chunk);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	private void queueTask(ChunkPosition position, Runnable runnable) {
		this.lock.lock();
		try {
			if (this.tasks.put(position, runnable) == null) {
				this.positions.offer(position);
			}

			if (this.suspended.compareAndSet(true, false)) {
				this.condition.signal();
			}
		} finally {
			this.lock.unlock();
		}
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
}
