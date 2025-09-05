package net.imprex.orebfuscator.util;

public class RingLongBuffer {

	private final long[] buffer;

	private int size;
	private int head;

	public RingLongBuffer(int size) {
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

	public synchronized void add(long time) {
		buffer[head++] = time;

		if (head >= buffer.length) {
			head = 0;
		}

		if (size < buffer.length) {
			size++;
		}
	}
}
