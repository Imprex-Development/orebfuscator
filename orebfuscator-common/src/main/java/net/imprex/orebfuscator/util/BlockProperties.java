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
			Objects.requireNonNull(this.defaultBlockState);
			Objects.requireNonNull(this.possibleBlockStates);

			return new BlockProperties(this);
		}
	}
}
