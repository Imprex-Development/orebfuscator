package net.imprex.orebfuscator.nms.v1_16_R1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R1.CraftServer;

import dev.imprex.orebfuscator.cache.AbstractRegionFileCache;
import dev.imprex.orebfuscator.config.api.CacheConfig;
import dev.imprex.orebfuscator.util.ChunkCacheKey;
import net.minecraft.server.v1_16_R1.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R1.RegionFile;
import net.minecraft.server.v1_16_R1.RegionFileCompression;

public class RegionFileCache extends AbstractRegionFileCache<RegionFile> {

	RegionFileCache(CacheConfig cacheConfig) {
		super(cacheConfig);
	}

	@Override
	protected RegionFile createRegionFile(Path path) throws IOException {
		boolean isSyncChunkWrites = ((CraftServer) Bukkit.getServer()).getServer().isSyncChunkWrites();
		return new RegionFile(path, path.getParent(), RegionFileCompression.c, isSyncChunkWrites);
	}

	@Override
	protected void closeRegionFile(RegionFile t) throws IOException {
		t.close();
	}

	@Override
	protected DataInputStream createInputStream(RegionFile t, ChunkCacheKey key) throws IOException {
		return t.a(new ChunkCoordIntPair(key.x(), key.z()));
	}

	@Override
	protected DataOutputStream createOutputStream(RegionFile t, ChunkCacheKey key) throws IOException {
		return t.c(new ChunkCoordIntPair(key.x(), key.z()));
	}
}