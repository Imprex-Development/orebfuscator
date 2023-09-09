package net.imprex.orebfuscator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.lang.StringUtils;

public final class ConsoleUtil {

	private static final int BOX_PADDING = 3;
	private static final int BOX_MAX_WIDTH = 48;

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

		List<String> wrappedLines = new ArrayList<>();
		for (String line : lines) {
			line = line.trim();

			while (line.length() > BOX_MAX_WIDTH) {

				int splitLength = 0;
				for (int i = 0; i < line.length(); i++) {
					if (Character.isWhitespace(line.charAt(i))) {
						if (i <= BOX_MAX_WIDTH) {
							splitLength = i;
						} else {
							break;
						}
					}
				}

				// split line at latest word that fit length
				wrappedLines.add(line.substring(0, splitLength));
				line = line.substring(splitLength, line.length()).trim();
			}

			// add remainder
			wrappedLines.add(line);
		}

		// get max line width
		int width = 0;
		for (String line : wrappedLines) {
			width = Math.max(width, line.length());
		}

		// add padding
		int totalWidth = width + BOX_PADDING * 2;

		// create top/bottom lines
		String bottomTopLine = StringUtils.repeat("═", totalWidth);
		String topLine = String.format("╔%s╗", bottomTopLine);
		String bottomLine = String.format("╚%s╝", bottomTopLine);

		// create box
		List<String> box = new ArrayList<>(wrappedLines.size() + 2);
		box.add(topLine);

		for (String line : wrappedLines) {
			int space = totalWidth - line.length();

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
