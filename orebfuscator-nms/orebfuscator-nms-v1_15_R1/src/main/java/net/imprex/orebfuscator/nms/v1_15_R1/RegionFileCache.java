package net.imprex.orebfuscator.nms.v1_15_R1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.minecraft.server.v1_15_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_15_R1.RegionFile;
import net.minecraft.server.v1_15_R1.RegionFileCompression;

public class RegionFileCache extends AbstractRegionFileCache<RegionFile> {

	RegionFileCache(CacheConfig cacheConfig) {
		super(cacheConfig);
	}

	@Override
	protected RegionFile createRegionFile(Path path) throws IOException {
		return new RegionFile(path, path.getParent(), RegionFileCompression.b);
	}

	@Override
	protected void closeRegionFile(RegionFile t) throws IOException {
		t.close();
	}

	@Override
	protected DataInputStream createInputStream(RegionFile t, ChunkPosition key) throws IOException {
		return t.a(new ChunkCoordIntPair(key.x, key.z));
	}

	@Override
	protected DataOutputStream createOutputStream(RegionFile t, ChunkPosition key) throws IOException {
		return t.c(new ChunkCoordIntPair(key.x, key.z));
	}
}