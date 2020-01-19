package net.imprex.orebfuscator.nms.v1_9_R1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.minecraft.server.v1_9_R1.RegionFile;

public class RegionFileCache extends AbstractRegionFileCache<RegionFile> {

	public RegionFileCache(CacheConfig cacheConfig) {
		super(cacheConfig);
	}

	@Override
	public DataInputStream getInputStream(Path path, ChunkPosition key) throws IOException {
		return this.get(path).a(key.getX() & 0x1F, key.getZ() & 0x1F);
	}

	@Override
	public DataOutputStream getOutputStream(Path path, ChunkPosition key) throws IOException {
		return this.get(path).b(key.getX() & 0x1F, key.getZ() & 0x1F);
	}

	@Override
	protected RegionFile createRegionFile(Path path) throws IOException {
		return new RegionFile(path.toFile());
	}

	@Override
	protected void closeRegionFile(RegionFile t) throws IOException {
		t.c();
	}
}