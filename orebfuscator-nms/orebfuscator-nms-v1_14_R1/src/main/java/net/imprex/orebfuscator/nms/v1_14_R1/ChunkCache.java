/**
 * @author lishid
 * @author Aleksey Terzi
 *
 */

package net.imprex.orebfuscator.nms.v1_14_R1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.HashMap;

import com.lishid.orebfuscator.nms.IChunkCache;

import net.minecraft.server.v1_14_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_14_R1.RegionFile;

public class ChunkCache implements IChunkCache {

	private static final HashMap<File, RegionFile> cachedRegionFiles = new HashMap<File, RegionFile>();

	private final int maxLoadedCacheFiles;

	public ChunkCache(int maxLoadedCacheFiles) {
		this.maxLoadedCacheFiles = maxLoadedCacheFiles;
	}

	@Override
	public DataInputStream getInputStream(File folder, int x, int z) {
		RegionFile regionFile = this.getRegionFile(folder, x, z);
		return regionFile.a(new ChunkCoordIntPair(x & 0x1F, z & 0x1F));
	}

	@Override
	public DataOutputStream getOutputStream(File folder, int x, int z) {
		RegionFile regionFile = this.getRegionFile(folder, x, z);
		return regionFile.c(new ChunkCoordIntPair(x & 0x1F, z & 0x1F));
	}

	@Override
	public void closeCacheFiles() {
		this.closeCacheFilesInternal();
	}

	private synchronized RegionFile getRegionFile(File folder, int x, int z) {
		File path = new File(folder, "region");
		File file = new File(path, "r." + (x >> 5) + "." + (z >> 5) + ".mcr");

		try {
			RegionFile regionFile = cachedRegionFiles.get(file);

			if (regionFile != null) {
				return regionFile;
			}

			if (!path.exists()) {
				path.mkdirs();
			}

			if (cachedRegionFiles.size() >= this.maxLoadedCacheFiles) {
				this.closeCacheFiles();
			}

			regionFile = new RegionFile(file);
			cachedRegionFiles.put(file, regionFile);

			return regionFile;
		} catch (Exception e) {
			try {
				file.delete();
			} catch (Exception e2) {
			}
		}
		return null;
	}

	private synchronized void closeCacheFilesInternal() {
		for (RegionFile regionFile : cachedRegionFiles.values()) {
			try {
				regionFile.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		cachedRegionFiles.clear();
	}
}
