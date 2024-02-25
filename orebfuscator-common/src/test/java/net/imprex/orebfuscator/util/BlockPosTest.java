package net.imprex.orebfuscator.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BlockPosTest {

	@Test
	public void testLongFormat() {
		BlockPos positionA = new BlockPos(-52134, BlockPos.MAX_Y, 6243234);
		BlockPos positionB = new BlockPos(0, BlockPos.MIN_Y, -4);
		BlockPos positionC = new BlockPos(15, 0, -5663423);
		BlockPos positionD = new BlockPos(21523, 16, -5663423);

		long valueA = positionA.toLong();
		long valueB = positionB.toLong();
		long valueC = positionC.toLong();
		long valueD = positionD.toLong();

		assertEquals(positionA, BlockPos.fromLong(valueA));
		assertEquals(positionB, BlockPos.fromLong(valueB));
		assertEquals(positionC, BlockPos.fromLong(valueC));
		assertEquals(positionD, BlockPos.fromLong(valueD));
	}

	@Test
	public void testSectionPos() {
		final int chunkX = -42 << 4;
		final int chunkZ = 6521 << 4;

		BlockPos positionA = new BlockPos(chunkX + 8, BlockPos.MAX_Y, chunkZ);
		BlockPos positionB = new BlockPos(chunkX, BlockPos.MIN_Y, chunkZ + 15);
		BlockPos positionC = new BlockPos(chunkX + 15, 0, chunkZ + 4);

		int sectionPosA = positionA.toSectionPos();
		int sectionPosB = positionB.toSectionPos();
		int sectionPosC = positionC.toSectionPos();

		assertEquals(positionA, BlockPos.fromSectionPos(chunkX, chunkZ, sectionPosA));
		assertEquals(positionB, BlockPos.fromSectionPos(chunkX, chunkZ, sectionPosB));
		assertEquals(positionC, BlockPos.fromSectionPos(chunkX, chunkZ, sectionPosC));
	}
}
