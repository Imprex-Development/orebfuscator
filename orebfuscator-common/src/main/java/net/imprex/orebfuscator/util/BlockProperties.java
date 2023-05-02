package net.imprex.orebfuscator.util;

import java.util.Objects;

import com.google.common.collect.ImmutableList;

public class BlockProperties {

	public static Builder builder(NamespacedKey key) {
		return new Builder(key);
	}

	private final NamespacedKey key;
	private final BlockStateProperties defaultBlockState;
	private final ImmutableList<BlockStateProperties> possibleBlockStates;

	private BlockProperties(Builder builder) {
		this.key = builder.key;
		this.defaultBlockState = builder.defaultBlockState;
		this.possibleBlockStates = builder.possibleBlockStates;
	}

	public NamespacedKey getKey() {
		return key;
	}

	public BlockStateProperties getDefaultBlockState() {
		return defaultBlockState;
	}

	public ImmutableList<BlockStateProperties> getPossibleBlockStates() {
		return possibleBlockStates;
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
		if (!(obj instanceof BlockProperties)) {
			return false;
		}
		BlockProperties other = (BlockProperties) obj;
		return Objects.equals(key, other.key);
	}

	@Override
	public String toString() {
		return "BlockProperties [key=" + key + ", defaultBlockState=" + defaultBlockState + ", possibleBlockStates="
				+ possibleBlockStates + "]";
	}

	public static class Builder {

		private final NamespacedKey key;

		private BlockStateProperties defaultBlockState;
		private ImmutableList<BlockStateProperties> possibleBlockStates;

		private Builder(NamespacedKey key) {
			this.key = key;
		}

		public Builder withDefaultBlockState(BlockStateProperties defaultBlockState) {
			this.defaultBlockState = defaultBlockState;
			return this;
		}

		public Builder withPossibleBlockStates(ImmutableList<BlockStateProperties> possibleBlockStates) {
			this.possibleBlockStates = possibleBlockStates;
			return this;
		}
		
		public BlockProperties build() {
			Objects.requireNonNull(this.defaultBlockState, "missing default block state for " + this.key);
			Objects.requireNonNull(this.possibleBlockStates, "missing possible block states for " + this.key);

			return new BlockProperties(this);
		}
	}
}
