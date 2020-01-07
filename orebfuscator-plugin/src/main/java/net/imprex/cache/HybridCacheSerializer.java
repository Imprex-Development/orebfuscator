package net.imprex.cache;

import java.io.IOException;

public interface HybridCacheSerializer<K, V> {

	V read(K key) throws IOException;

	void write(K key, V value) throws IOException;
}
