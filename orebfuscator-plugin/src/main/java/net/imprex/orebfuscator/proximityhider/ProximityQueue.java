package net.imprex.orebfuscator.proximityhider;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.entity.Player;

public class ProximityQueue {

	private final Lock lock = new ReentrantLock();
	private final Condition notEmpty = lock.newCondition();

	private final Queue<Player> queue = new LinkedList<>();
	private final Set<Player> lockedPlayer = new HashSet<>();

	public void offerAndLock(Player player) {
		Objects.requireNonNull(player);

		lock.lock();
		try {
			if (this.lockedPlayer.add(player)) {
				boolean empty = this.queue.isEmpty();
				this.queue.offer(player);
				if (empty) {
					this.notEmpty.signal();
				}
			}
		} finally {
			lock.unlock();
		}
	}

	public Player poll() throws InterruptedException {
		lock.lock();
		try {
			if (this.queue.isEmpty()) {
				this.notEmpty.await();
			}
			return this.queue.poll();
		} finally {
			lock.unlock();
		}
	}

	public void unlock(Player player) {
		lock.lock();
		try {
			this.lockedPlayer.remove(player);
		} finally {
			lock.unlock();
		}
	}

	public void remove(Player player) {
		lock.lock();
		try {
			if (this.lockedPlayer.remove(player)) {
				this.queue.remove(player);
			}
		} finally {
			lock.unlock();
		}
	}

	public void clear() {
		lock.lock();
		try {
			this.lockedPlayer.clear();
			this.queue.clear();
		} finally {
			lock.unlock();
		}
	}
}
