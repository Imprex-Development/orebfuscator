/*
 * Copyright (C) 2011-2014 lishid.  All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation,  version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.lishid.orebfuscator.obfuscation;

import java.util.zip.CRC32;

public class CalculationsUtil {

	public static long Hash(byte[] data, int length, byte[] configHash) {
		CRC32 crc = new CRC32();
		crc.reset();
		crc.update(data, 0, length);
		crc.update(configHash);
		return crc.getValue();
	}

	public static int increment(int current, int max) {
		return (current + 1) % max;
	}
}
