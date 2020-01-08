/**
 * @author lishid
 * @author Aleksey Terzi
 *
 */

package net.imprex.orebfuscator.nms.v1_15_R1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import com.lishid.orebfuscator.nms.IChunkCache;

import net.minecraft.server.v1_15_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_15_R1.RegionFile;
import net.minecraft.server.v1_15_R1.RegionFileCompression;

public class ChunkCache implements IChunkCache {

	private static final HashMap<Path, RegionFile> cachedRegionFiles = new HashMap<Path, RegionFile>();

	private final int maxLoadedCacheFiles;

	public ChunkCache(int maxLoadedCacheFiles) {
		this.maxLoadedCacheFiles = maxLoadedCacheFiles;
	}

	@Override
	public DataInputStream getInputStream(File folder, int x, int z) throws IOException {
		RegionFile regionFile = this.getRegionFile(folder, x, z);
		return regionFile.a(new ChunkCoordIntPair(x & 0x1F, z & 0x1F));
	}

	@Override
	public DataOutputStream getOutputStream(File folder, int x, int z) throws IOException {
		RegionFile regionFile = this.getRegionFile(folder, x, z);
		return regionFile.c(new ChunkCoordIntPair(x & 0x1F, z & 0x1F));
	}

	@Override
	public synchronized void closeCacheFiles() {
		for (RegionFile regionFile : ChunkCache.cachedRegionFiles.values()) {
			try {
				regionFile.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		ChunkCache.cachedRegionFiles.clear();
	}

	private synchronized RegionFile getRegionFile(File folder, int x, int z) throws IOException {
		Path path = folder.toPath().resolve("region");
		Path file = Paths.get(path.toString(), "r." + (x >> 5) + "." + (z >> 5) + ".mcr");

		try {
			RegionFile regionFile = ChunkCache.cachedRegionFiles.get(file);

			if (regionFile != null) {
				return regionFile;
			}

			if (!Files.exists(path)) {
				Files.createDirectories(path);
			}

			if (ChunkCache.cachedRegionFiles.size() >= this.maxLoadedCacheFiles) {
				this.closeCacheFiles();
			}

			regionFile = new RegionFile(file, path, RegionFileCompression.b);
			ChunkCache.cachedRegionFiles.put(file, regionFile);

			return regionFile;
		} catch (IOException e) {
			try {
				Files.delete(file);
			} catch (Exception e2) {
				e2.printStackTrace();
			}
			throw e;
		}
	}
}