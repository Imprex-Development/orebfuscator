package net.imprex.orebfuscator.config.components;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.config.ConfigParsingContext;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.BlockProperties;
import net.imprex.orebfuscator.util.HeightAccessor;
import net.imprex.orebfuscator.util.MathUtil;
import net.imprex.orebfuscator.util.OFCLogger;
import net.imprex.orebfuscator.util.WeightedIntRandom;

public class WeightedBlockList {

	public static WeightedIntRandom[] create(HeightAccessor heightAccessor, List<WeightedBlockList> lists) {
		WeightedIntRandom[] heightMap = new WeightedIntRandom[heightAccessor.getHeight()];

		List<WeightedBlockList> last = new ArrayList<>();
		List<WeightedBlockList> next = new ArrayList<>();

		int count = 0;

		for (int y = heightAccessor.getMinBuildHeight(); y < heightAccessor.getMaxBuildHeight(); y++) {
			for (WeightedBlockList list : lists) {
				if (list.minY <= y && list.maxY >= y) {
					next.add(list);
				}
			}

			int index = y - heightAccessor.getMinBuildHeight();
			if (index > 0 && last.equals(next)) {
				// copy last random
				heightMap[index] = heightMap[index - 1];
			} else {
				WeightedIntRandom.Builder builder = WeightedIntRandom.builder();

				for (WeightedBlockList list : next) {
					for (Map.Entry<BlockProperties, Integer> entry : list.blocks.entrySet()) {
						if (!builder.add(entry.getKey().getDefaultBlockState().getId(), entry.getValue())) {
							OFCLogger.warn(String.format("duplicate randomBlock entry for %s in %s",
									entry.getKey().getKey(), list.name));
						}
					}
				}

				heightMap[index] = builder.build();
				count++;

				// update last only on change
				last.clear();
				last.addAll(next);
			}

			next.clear();
		}

		OFCLogger.debug(String.format("Successfully created %s weigthed randoms", count));

		return heightMap;
	}

    private final String name;

    private final int minY;
    private final int maxY;

    private final Map<BlockProperties, Integer> blocks = new LinkedHashMap<>();

    public WeightedBlockList(ConfigParsingContext context, ConfigurationSection section) {
        this.name = section.getName();

        int minY = MathUtil.clamp(section.getInt("minY", BlockPos.MIN_Y), BlockPos.MIN_Y, BlockPos.MAX_Y);
        int maxY = MathUtil.clamp(section.getInt("maxY", BlockPos.MAX_Y), BlockPos.MIN_Y, BlockPos.MAX_Y);

        this.minY = Math.min(minY, maxY);
        this.maxY = Math.max(minY, maxY);

        if (!section.isConfigurationSection("blocks")) {
            context.failMissingOrEmpty(section, "blocks");
            return;
        }

        ConfigurationSection blockSection = section.getConfigurationSection("blocks");
        for (String blockName : blockSection.getKeys(false)) {
        	BlockProperties blockProperties = NmsInstance.getBlockByName(blockName);
            if (blockProperties != null) {
                int weight = blockSection.getInt(blockName, 1);
                this.blocks.put(blockProperties, weight);
            } else {
                context.warnUnknownBlock(section, "blocks", blockName);
            }
        }

        if (this.blocks.isEmpty()) {
            context.failMissingOrEmpty(section, "blocks");
        }
    }

    public void serialize(ConfigurationSection section) {
        section = section.createSection(this.name);

        section.set("minY", this.minY);
        section.set("maxY", this.maxY);

        section = section.createSection("blocks");
        for (Map.Entry<BlockProperties, Integer> entry : this.blocks.entrySet()) {
            section.set(entry.getKey().getKey().toString(), entry.getValue());
        }
    }
   
    public Set<BlockProperties> getBlocks() {
    	return Collections.unmodifiableSet(this.blocks.keySet());
    }
}
