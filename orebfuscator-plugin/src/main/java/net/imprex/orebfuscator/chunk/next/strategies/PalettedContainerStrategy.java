package net.imprex.orebfuscator.chunk.next.strategies;

import java.util.List;
import java.util.function.IntFunction;

import io.netty.buffer.ByteBuf;
import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.chunk.ChunkCapabilities;
import net.imprex.orebfuscator.chunk.next.palette.DirectPalette;
import net.imprex.orebfuscator.chunk.next.palette.IndirectPalette;
import net.imprex.orebfuscator.chunk.next.palette.Palette;
import net.imprex.orebfuscator.chunk.next.palette.SingleValuedPalette;
import net.imprex.orebfuscator.util.MathUtil;

public class PalettedContainerStrategy {

	public static final PalettedContainerStrategy BLOCK_STATES = new PalettedContainerStrategy(4, OrebfuscatorNms.getMaxBitsPerBlockState(),
		bits -> {
			if (ChunkCapabilities.hasSingleValuePalette() && bits == 0) {
				return Configuration.singleValued();
			} else if (bits < 9) {
				return Configuration.indirect(Math.max(4, bits));
			} else {
				return Configuration.direct(OrebfuscatorNms.getMaxBitsPerBlockState());
			}
		});

	public static final PalettedContainerStrategy BIOMES = new PalettedContainerStrategy(2, OrebfuscatorNms.getMaxBitsPerBiome(),
		bits -> {
			if (ChunkCapabilities.hasSingleValuePalette() && bits == 0) {
				return Configuration.singleValued();
			} else if (bits < 4) {
				return Configuration.indirect(bits);
			} else {
				return Configuration.direct(OrebfuscatorNms.getMaxBitsPerBiome());
			}
		});

	private final int sizeBits;
	private final Configuration[] configurations;

	private PalettedContainerStrategy(int sizeBits, int maxBitsPerEntry, IntFunction<Configuration> configure) {
		this.sizeBits = sizeBits;

		this.configurations = new Configuration[maxBitsPerEntry + 1];
		for (int bits = 0; bits <= maxBitsPerEntry; bits++) {
			this.configurations[bits] = configure.apply(bits);
		}
	}

	public int size() {
		return 1 << (this.sizeBits * 3);
	}

	public int getIndex(int x, int y, int z) {
		int mask = (1 << this.sizeBits) - 1;
		return ((y & mask) << this.sizeBits | (z & mask)) << this.sizeBits | (x & mask);
	}

	public Palette.Builder createBuilder() {
		return new PaletteBuilder(this);
	}

	Palette createPalette(List<Integer> values) {
		int bits = values.size() == 1
			? 0
			: MathUtil.ceilLog2(values.size());
		return getConfiguration(bits).createPalette(values);
	}

	public Configuration getConfiguration(int bits) {
		return bits > this.configurations.length
			? this.configurations[this.configurations.length - 1]
			: this.configurations[bits];
	}

	public static record Configuration(Palette.Factory factory, int bits) {

		private static Configuration singleValued() {
			return new Configuration(SingleValuedPalette.FACTORY, 0);
		}

		private static Configuration indirect(int bits) {
			return new Configuration(IndirectPalette.FACTORY, bits);
		}

		private static Configuration direct(int maxBitsPerEntry) {
			return new Configuration(DirectPalette.createFactory(maxBitsPerEntry), maxBitsPerEntry);
		}

		public void skipBytes(ByteBuf buffer) {
			this.factory.skipBytes(buffer);
		}

		public Palette read(ByteBuf buffer) {
			return this.factory.read(buffer, this.bits);
		}

		private Palette createPalette(List<Integer> values) {
			return this.factory.create(this.bits, values);
		}
	}
}
