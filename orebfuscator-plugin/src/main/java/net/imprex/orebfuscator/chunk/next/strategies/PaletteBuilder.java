package net.imprex.orebfuscator.chunk.next.strategies;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import net.imprex.orebfuscator.chunk.next.palette.Palette;

public class PaletteBuilder implements Palette.Builder {

	/**
	 * TODO: rewrite to return index on value add to allow
	 * for fast write without remapping, discard index[] if
	 * size exceeds 8 bit;
	 * 
	 * Use a hash bucket strategy with a power-2 growth until
	 * max size;
	 */

	private final PalettedContainerStrategy strategy;

	private final BitSet byValue;
	private final List<Integer> byIndex;

	public PaletteBuilder(PalettedContainerStrategy strategy) {
		this.strategy = strategy;

		this.byValue = new BitSet();
		this.byIndex = new ArrayList<>();
	}

	@Override
	public PaletteBuilder add(int value) {
		if (this.byIndex.size() > 256) {
			return this;
		}
		
		if (!this.byValue.get(value)) {
			this.byValue.set(value);
			this.byIndex.add(value);
		}

		return this;
	}

	@Override
	public Palette build() {
		return this.strategy.createPalette(this.byIndex);
	}
}
