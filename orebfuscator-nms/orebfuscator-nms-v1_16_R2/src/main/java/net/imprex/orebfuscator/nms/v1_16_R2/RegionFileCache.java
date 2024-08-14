package net.imprex.orebfuscator.nms.v1_16_R2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_16_R2.CraftServer;

import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.minecraft.server.v1_16_R2.ChunkCoordIntPair;
import net.minecraft.server.v1_16_R2.RegionFile;
import net.minecraft.server.v1_16_R2.RegionFileCompression;

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
	protected DataInputStream createInputStream(RegionFile t, ChunkPosition key) throws IOException {
		return t.a(new ChunkCoordIntPair(key.x, key.z));
	}

	@Override
	protected DataOutputStream createOutputStream(RegionFile t, ChunkPosition key) throws IOException {
		return t.c(new ChunkCoordIntPair(key.x, key.z));
	}
}