package dev.imprex.orebfuscator.util;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public record BlockTag(@NotNull NamespacedKey key, @NotNull Set<BlockProperties> blocks) {

	public BlockTag(@NotNull NamespacedKey key, @NotNull Set<BlockProperties> blocks) {
		this.key = key;
		this.blocks = Collections.unmodifiableSet(blocks);
	}

	@Override
	public int hashCode() {
		return this.key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof BlockTag other)) {
			return false;
		}
		return Objects.equals(key, other.key);
	}

	@Override
	public String toString() {
		return "BlockTag [key=" + key + ", blocks=" + blocks + "]";
	}
}
