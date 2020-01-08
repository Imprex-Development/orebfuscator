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

package com.lishid.orebfuscator.cache;

import java.io.File;

public class ObfuscatedCachedChunk {
	File path;
	int x;
	int z;
	public byte[] data;
	public int[] proximityList;
	public int[] removedEntityList;
	public long hash = 0L;
	private boolean loaded = false;

	public ObfuscatedCachedChunk(File file, int x, int z) {
		this.x = x;
		this.z = z;
		this.path = new File(file, "data");
		this.path.mkdirs();
	}

	public void invalidate() {
		this.write(0L, new byte[0], new int[0], new int[0]);
	}

	public void free() {
		this.data = null;
		this.proximityList = null;
		this.removedEntityList = null;
	}

	public long getHash() {
		this.read();

		if (!this.loaded) {
			return 0L;
		}

		return this.hash;
	}

	public void read() { }

	public void write(long hash, byte[] data, int[] proximityList, int[] removedEntityList) { }
}