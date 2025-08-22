package net.imprex.orebfuscator.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import dev.imprex.orebfuscator.config.ProximityHeightCondition;

public class ProximityHeightConditionTest {

	private static final int TEST_MIN = ProximityHeightCondition.clampY(-0xFFF);
	private static final int TEST_MAX = ProximityHeightCondition.clampY(0xFFF);

	@Test
	public void testCreateRemove() {
		final int minY = -10;
		final int maxY = 10;

		int flag = 0b101;
		assertFalse(ProximityHeightCondition.isPresent(flag));

		flag |= ProximityHeightCondition.create(minY, maxY);
		assertTrue(ProximityHeightCondition.isPresent(flag));
		assertEquals(minY, ProximityHeightCondition.getMinY(flag));
		assertEquals(maxY, ProximityHeightCondition.getMaxY(flag));

		for (int y = TEST_MIN; y <= TEST_MAX; y++) {
			boolean expected = minY <= y && maxY >= y;
			assertEquals(expected, ProximityHeightCondition.match(flag, y), "failed for " + y);
		}

		int other = ProximityHeightCondition.create(minY, maxY);
		assertTrue(ProximityHeightCondition.equals(flag, other));

		flag = ProximityHeightCondition.remove(flag);
		assertEquals(0b101, flag);
	}

	@Test
	public void testReadWrite() {
		for (int minY = TEST_MIN; minY <= TEST_MAX; minY++) {
			for (int maxY = minY; maxY <= TEST_MAX; maxY++) {
				int flag = ProximityHeightCondition.create(minY, maxY);

				assertTrue(ProximityHeightCondition.isPresent(flag));
				assertEquals(minY, ProximityHeightCondition.getMinY(flag));
				assertEquals(maxY, ProximityHeightCondition.getMaxY(flag));
			}
		}
	}
}
