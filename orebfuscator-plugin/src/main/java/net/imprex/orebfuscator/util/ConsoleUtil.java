package net.imprex.orebfuscator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;

public final class ConsoleUtil {

	private static final int BOX_PADDING = 4;

	private ConsoleUtil() {
	}

	public static void printBox(Level level, String...lines) {
		for (String line : createBox(lines)) {
			OFCLogger.log(level, line);
		}
	}

	/**
	 * Creates a ASCII box around the given lines
	 */
	public static Iterable<String> createBox(String...lines) {
		// get max line width
		int width = 0;
		for (String line : lines) {
			width = Math.max(width, line.length());
		}

		// add padding
		width += BOX_PADDING * 2;

		// create top/bottom lines
		String bottomTopLine = StringUtils.repeat("═", width);
		String topLine = String.format("╔%s╗", bottomTopLine);
		String bottomLine = String.format("╚%s╝", bottomTopLine);

		// create box
		List<String> box = new ArrayList<>(lines.length + 2);
		box.add(topLine);

		for (String line : lines) {
			int space = width - line.length();

			// center line
			String leftPadding, rightPadding;
			if (space % 2 == 0) {
				leftPadding = rightPadding = StringUtils.repeat(" ", space / 2);
			} else {
				leftPadding = StringUtils.repeat(" ", space / 2 + 1);
				rightPadding = StringUtils.repeat(" ", space / 2);
			}

			box.add(String.format("║%s%s%s║", leftPadding, line, rightPadding));
		}

		box.add(bottomLine);
		return box;
	}
}
