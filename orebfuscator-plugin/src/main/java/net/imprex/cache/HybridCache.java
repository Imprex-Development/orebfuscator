package net.imprex.cache;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalNotification;

/**
 * A semi-persistent mapping from keys to values. Values are automatically loaded by the cache, and
 * are stored in the cache until their evicted and stored to disk for later retrieval.
 */
public class HybridCache<K, V> {

	private final LoadingCache<K, Optional<V>> cache = CacheBuilder.newBuilder()
			.maximumSize(65_536L)
			.expireAfterAccess(30, TimeUnit.SECONDS)
			.removalListener(this::onRemoval)
			.build(CacheLoader.from(this::load));

	private final HybridCacheSerializer<K, V> serializer;

	/**
	 * Creates a new HybridCache
	 * @param serializer serializer to store or retrieve key value pairs from disk
	 */
	public HybridCache(HybridCacheSerializer<K, V> serializer) {
		this.serializer = serializer;
	}

	// If there was an automatic removal due to eviction and a value is present
	// we write the object to disk cache
	private void onRemoval(RemovalNotification<K, Optional<V>> notification) {
		if (notification.wasEvicted()) {
			Optional<V> value = notification.getValue();
			if (value.isPresent()) {
				try {
					this.serializer.write(notification.getKey(), value.get());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	// If the key has no corresponding value we use an empty optional
	// (since null values aren't allowed)
	private Optional<V> load(K key) {
		Optional<V> value;
		try {
			value = Optional.ofNullable(this.serializer.read(key));
		} catch (IOException e) {
			e.printStackTrace();
			value = Optional.empty();
		}
		return value;
	}

	/**
	 * If the specified key is not already associated with a value (or is mapped to
	 * {@code null}), attempts to compute its value using the given mapping function
	 * and enters it into this cache unless {@code null}.
	 * 
	 * @param key key with which the specified value is to be associated
	 * @param mappingFunction the function to compute a value
	 * @return the current (existing or computed) value
	 * 
	 * @throws NullPointerException if the specified key is null or the
	 *                              mappingFunction is or returns null
	 */
	public V get(K key, Function<? super K, ? extends V> mappingFunction) {
		Objects.requireNonNull(mappingFunction);

		try {
			Optional<V> optional = this.cache.get(key);
			if (optional.isPresent()) {
				return optional.get();
			} else {
				V value = mappingFunction.apply(key);
				this.cache.put(key, Optional.of(value));
				return value;
			}
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}
}
