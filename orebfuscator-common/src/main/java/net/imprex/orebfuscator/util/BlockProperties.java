package net.imprex.orebfuscator.util;

import java.util.Objects;

import com.google.common.collect.ImmutableList;

public class BlockProperties {

	public static Builder builder(String name) {
		return new Builder(name);
	}

	private final String name;
	private final BlockStateProperties defaultBlockState;
	private final ImmutableList<BlockStateProperties> possibleBlockStates;

	private BlockProperties(Builder builder) {
		this.name = builder.name;
		this.defaultBlockState = builder.defaultBlockState;
		this.possibleBlockStates = builder.possibleBlockStates;
	}

	public String getName() {
		return name;
	}

	public BlockStateProperties getDefaultBlockState() {
		return defaultBlockState;
	}

	public ImmutableList<BlockStateProperties> getPossibleBlockStates() {
		return possibleBlockStates;
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
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
		return Objects.equals(name, other.name);
	}

	@Override
	public String toString() {
		return "BlockProperties [name=" + name + ", defaultBlockState=" + defaultBlockState + ", possibleBlockStates="
				+ possibleBlockStates + "]";
	}

	public static class Builder {

		private final String name;

		private BlockStateProperties defaultBlockState;
		private ImmutableList<BlockStateProperties> possibleBlockStates;

		private Builder(String name) {
			this.name = name;
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
			Objects.requireNonNull(this.defaultBlockState, "missing default block state for " + this.name);
			Objects.requireNonNull(this.possibleBlockStates, "missing possible block states for " + this.name);

			return new BlockProperties(this);
		}
	}
}