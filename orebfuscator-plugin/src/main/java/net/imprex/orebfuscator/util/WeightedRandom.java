package net.imprex.orebfuscator.util;

import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

public class WeightedRandom<T> {

	private final NavigableMap<Integer, T> map = new TreeMap<Integer, T>();
	private int total = 0;

	public WeightedRandom<T> add(int weight, T result) {
		if (weight <= 0)
			return this;
		this.total += weight;
		this.map.put(this.total, result);
		return this;
	}

	public T next() {
		int value = ThreadLocalRandom.current().nextInt(this.total);
		return this.map.higherEntry(value).getValue();
	}
}
