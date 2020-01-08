/**
 * @author lishid
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.nms.v1_9_R1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import com.lishid.orebfuscator.nms.IChunkCache;

import net.minecraft.server.v1_9_R1.RegionFile;

public class ChunkCache implements IChunkCache {

	private static final HashMap<Path, RegionFile> cachedRegionFiles = new HashMap<Path, RegionFile>();

	private final int maxLoadedCacheFiles;

	public ChunkCache(int maxLoadedCacheFiles) {
		this.maxLoadedCacheFiles = maxLoadedCacheFiles;
	}

	@Override
	public DataInputStream getInputStream(File folder, int x, int z) {
		RegionFile regionFile = this.getRegionFile(folder, x, z);
		return regionFile.a(x & 0x1F, z & 0x1F);
	}

	@Override
	public DataOutputStream getOutputStream(File folder, int x, int z) {
		RegionFile regionFile = this.getRegionFile(folder, x, z);
		return regionFile.b(x & 0x1F, z & 0x1F);
	}

	@Override
	public synchronized void closeCacheFiles() {
		for (RegionFile regionFile : cachedRegionFiles.values()) {
			try {
				if (regionFile != null) {
					regionFile.c();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		cachedRegionFiles.clear();
	}

	private synchronized RegionFile getRegionFile(File folder, int x, int z) {
		Path path = folder.toPath().resolve("region");
		Path file = Paths.get(path.toString(), "r." + (x >> 5) + "." + (z >> 5) + ".mcr");

		try {
			RegionFile regionFile = cachedRegionFiles.get(file);
			if (regionFile != null) {
				return regionFile;
			}

			if (!Files.isDirectory(path)) {
				Files.createDirectory(path);
			}

			if (cachedRegionFiles.size() >= this.maxLoadedCacheFiles) {
				this.closeCacheFiles();
			}

			regionFile = new RegionFile(file.toFile());
			ChunkCache.cachedRegionFiles.put(file, regionFile);

			return regionFile;
		} catch (Exception e) {
			try {
				Files.delete(file);
			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return null;
	}
}