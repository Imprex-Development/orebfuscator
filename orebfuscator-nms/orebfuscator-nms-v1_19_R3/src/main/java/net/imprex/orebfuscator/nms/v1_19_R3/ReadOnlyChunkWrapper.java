package net.imprex.orebfuscator.nms.v1_19_R3;

import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class ReadOnlyChunkWrapper implements ReadOnlyChunk {

	private final LevelChunk chunk;

	ReadOnlyChunkWrapper(LevelChunk chunk) {
		this.chunk = chunk;
	}

	private BlockState getState(int x, int y, int z) {
		int sectionIndex = chunk.getSectionIndex(y);
		LevelChunkSection[] sections = chunk.getSections();
		if (sectionIndex >= 0 && sectionIndex < sections.length) {
			LevelChunkSection section = sections[sectionIndex];
			if (sections != null && !section.hasOnlyAir()) {
				return section.getBlockState(x & 0xF, y & 0xF, z & 0xF);
			}
		}
		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public int getBlockState(int x, int y, int z) {
		return Block.getId(getState(x, y, z));
	}
}
