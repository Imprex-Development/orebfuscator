package net.imprex.orebfuscator.nms.v1_16_R1;

import net.imprex.orebfuscator.nms.ReadOnlyChunk;
import net.minecraft.server.v1_16_R1.Blocks;
import net.minecraft.server.v1_16_R1.Chunk;
import net.minecraft.server.v1_16_R1.ChunkSection;
import net.minecraft.server.v1_16_R1.IBlockData;

public class ReadOnlyChunkWrapper implements ReadOnlyChunk {

	private final Chunk chunk;

	ReadOnlyChunkWrapper(Chunk chunk) {
		this.chunk = chunk;
	}

	private IBlockData getState(int x, int y, int z) {
		ChunkSection[] sections = chunk.getSections();
		if (y >= 0 && y >> 4 < sections.length) {
			ChunkSection section = sections[y >> 4];
			if (!ChunkSection.a(section)) {
				return section.getType(x & 0xF, y & 0xF, z & 0xF);
			}
		}
		return Blocks.AIR.getBlockData();
	}

	@Override
	public int get(int x, int y, int z) {
		return NmsManager.getBlockId(getState(x, y, z));
	}
}
