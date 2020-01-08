/**
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.config;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.lishid.orebfuscator.NmsInstance;
import com.lishid.orebfuscator.utils.Globals;
import com.lishid.orebfuscator.utils.MaterialHelper;

public class WorldConfig {

	private final ProximityHiderConfig proximityHiderConfig = new ProximityHiderConfig();

	private String name;
	private Boolean enabled = false;
	private Boolean darknessHideBlocks;
	private Boolean antiTexturePackAndFreecam;
	private Boolean bypassObfuscationForSignsWithText;
	private Integer airGeneratorMaxChance;
	private HashSet<Integer> obfuscateBlocks;
	private HashSet<Integer> darknessBlocks;
	private byte[] obfuscateAndProximityBlocks;
	private Integer[] randomBlocks;
	private Integer[] randomBlocks2;
	private Integer mode1BlockId;
	private int[] paletteBlocks;

	protected void setObfuscateAndProximityBlocks() {
		this.obfuscateAndProximityBlocks = new byte[MaterialHelper.getMaxId() + 1];

		this.setObfuscateMask(this.obfuscateBlocks, false, false);

		if (this.darknessBlocks != null && this.darknessHideBlocks) {
			this.setObfuscateMask(this.darknessBlocks, true, false);
		}

		if (this.proximityHiderConfig != null && this.proximityHiderConfig.isEnabled()) {
			this.setObfuscateMask(this.proximityHiderConfig.getProximityHiderBlocks(), false, true);
		}
	}

	protected void setPaletteBlocks() {
		if (this.randomBlocks == null) {
			return;
		}

		HashSet<Integer> map = new HashSet<>();

		map.add(NmsInstance.current.getCaveAirBlockId());
		map.add(this.mode1BlockId);

		if (this.proximityHiderConfig.isUseSpecialBlock()) {
			map.add(this.proximityHiderConfig.getSpecialBlockID());
		}

		for (Integer id : this.randomBlocks) {
			if (id != null) {
				map.add(id);
			}
		}

		int[] paletteBlocks = new int[map.size()];
		int index = 0;

		for (Integer id : map) {
			paletteBlocks[index++] = id;
		}

		this.paletteBlocks = paletteBlocks;
	}

	public void shuffleRandomBlocks() {
		synchronized (this.randomBlocks) {
			Collections.shuffle(Arrays.asList(this.randomBlocks));
			Collections.shuffle(Arrays.asList(this.randomBlocks2));
		}
	}

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public Boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(Boolean value) {
		this.enabled = value;
	}

	public Boolean isDarknessHideBlocks() {
		return this.darknessHideBlocks;
	}

	public void setDarknessHideBlocks(Boolean value) {
		this.darknessHideBlocks = value;
	}

	public Boolean isAntiTexturePackAndFreecam() {
		return this.antiTexturePackAndFreecam;
	}

	public void setAntiTexturePackAndFreecam(Boolean value) {
		this.antiTexturePackAndFreecam = value;
	}

	public Boolean isBypassObfuscationForSignsWithText() {
		return this.bypassObfuscationForSignsWithText;
	}

	public void setBypassObfuscationForSignsWithText(Boolean value) {
		this.bypassObfuscationForSignsWithText = value;
	}

	public Integer getAirGeneratorMaxChance() {
		return this.airGeneratorMaxChance;
	}

	public void setAirGeneratorMaxChance(Integer value) {
		this.airGeneratorMaxChance = value;
	}

	public HashSet<Integer> getObfuscateBlocks() {
		return this.obfuscateBlocks;
	}

	public void setObfuscateBlocks(HashSet<Integer> value) {
		this.obfuscateBlocks = value;
	}

	public void setObfuscateBlocks(Integer[] value) {
		this.obfuscateBlocks = new HashSet<>();

		for (int id : value) {
			this.obfuscateBlocks.add(id);
		}
	}

	private void setObfuscateMask(Set<Integer> blockIds, boolean isDarknessBlock, boolean isProximityHider) {
		for (int blockId : blockIds) {
			int bits = this.obfuscateAndProximityBlocks[blockId] | Globals.MASK_OBFUSCATE;

			if (isDarknessBlock) {
				bits |= Globals.MASK_DARKNESSBLOCK;
			}

			if (isProximityHider) {
				bits |= Globals.MASK_PROXIMITYHIDER;
			}

			if (NmsInstance.current.isTileEntity(blockId)) {
				bits |= Globals.MASK_TILEENTITY;
			}

			this.obfuscateAndProximityBlocks[blockId] = (byte) bits;
		}
	}

	public byte[] getObfuscateAndProximityBlocks() {
		return this.obfuscateAndProximityBlocks;
	}

	public HashSet<Integer> getDarknessBlocks() {
		return this.darknessBlocks;
	}

	public void setDarknessBlocks(HashSet<Integer> values) {
		this.darknessBlocks = values;
	}

	public Integer[] getRandomBlocks() {
		return this.randomBlocks;
	}

	public void setRandomBlocks(Integer[] values) {
		this.randomBlocks = values;
		this.randomBlocks2 = values;
	}

	public Integer getMode1BlockId() {
		return this.mode1BlockId;
	}

	public void setMode1BlockId(Integer value) {
		this.mode1BlockId = value;
	}

	public int[] getPaletteBlocks() {
		return this.paletteBlocks;
	}

	public ProximityHiderConfig getProximityHiderConfig() {
		return this.proximityHiderConfig;
	}

	public int getObfuscatedBits(int id) {
		return this.obfuscateAndProximityBlocks[id];
	}

	public int getRandomBlock(int index, boolean alternate) {
		return alternate ? this.randomBlocks2[index] : this.randomBlocks[index];
	}
}
