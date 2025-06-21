package net.imprex.orebfuscator.util;

public class RingTimer {

	private final long[] buffer;

	private int size;
	private int head;

	private Long time;

	public RingTimer(int size) {
		this.buffer = new long[size];
	}

	public double average() {
		if (size == 0) {
			return 0;
		}

		double sum = 0;
		for (int i = 0; i < size; i++) {
			sum += buffer[i];
		}

		return sum / size;
	}

	public void start() {
		if (time == null) {
			time = System.nanoTime();
		}
	}

	public void stop() {
		if (time != null) {
			add(System.nanoTime() - time);
			time = null;
		}
	}

	public void add(long time) {
		buffer[head++] = time;

		if (head >= buffer.length) {
			head = 0;
		}

		if (size < buffer.length) {
			size++;
		}
	}
}
