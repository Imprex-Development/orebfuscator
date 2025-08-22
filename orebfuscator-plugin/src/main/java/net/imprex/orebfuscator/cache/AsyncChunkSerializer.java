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

import dev.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.Orebfuscator;

/**
 * This class works similar to a bounded buffer for cache read and write
 * requests but also functions as the only consumer of said buffer. All requests
 * can get reorder similar to modern memory access reordering in CPUs. If for
 * example a write request is already in the buffer and a new read request for
 * the same position is created then the read request doesn't get put in the
 * buffer and gets completed with the content of the write request.
 * 
 * @see <a href="https://en.wikipedia.org/wiki/Producer–consumer_problem">Bound buffer</a>
 * @see <a href="https://en.wikipedia.org/wiki/Memory_ordering">Memory ordering</a>
 */
public class AsyncChunkSerializer implements Runnable {

	private final Lock lock = new ReentrantLock(true);
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

		orebfuscator.getStatistics().setDiskCacheQueueLengthSupplier(() -> this.tasks.size());
	}

	public CompletableFuture<CompressedObfuscationResult> read(ChunkPosition position) {
		this.lock.lock();
		try {
			Runnable task = this.tasks.get(position);
			if (task instanceof WriteTask) {
				return CompletableFuture.completedFuture(((WriteTask) task).chunk);
			} else if (task instanceof ReadTask) {
				return ((ReadTask) task).future;
			} else {
				CompletableFuture<CompressedObfuscationResult> future = new CompletableFuture<>();
				this.queueTask(position, new ReadTask(position, future));
				return future;
			}
		} finally {
			this.lock.unlock();
		}
	}

	public void write(ChunkPosition position, CompressedObfuscationResult chunk) {
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
				while (this.positions.isEmpty()) {
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
		private final CompressedObfuscationResult chunk;

		public WriteTask(ChunkPosition position, CompressedObfuscationResult chunk) {
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
		private final CompletableFuture<CompressedObfuscationResult> future;

		public ReadTask(ChunkPosition position, CompletableFuture<CompressedObfuscationResult> future) {
			this.position = position;
			this.future = future;
		}

		@Override
		public void run() {
			try {
				future.complete(ChunkSerializer.read(position));
			} catch (IOException e) {
				future.completeExceptionally(e);
			}
		}
	}
}
