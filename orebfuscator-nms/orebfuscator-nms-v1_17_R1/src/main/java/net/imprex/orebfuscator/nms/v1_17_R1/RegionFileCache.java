package net.imprex.orebfuscator.nms.v1_17_R1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.CraftServer;

import dev.imprex.orebfuscator.cache.AbstractRegionFileCache;
import dev.imprex.orebfuscator.config.api.CacheConfig;
import dev.imprex.orebfuscator.util.ChunkPosition;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.storage.RegionFile;
import net.minecraft.world.level.chunk.storage.RegionFileVersion;

public class RegionFileCache extends AbstractRegionFileCache<RegionFile> {

	RegionFileCache(CacheConfig cacheConfig) {
		super(cacheConfig);
	}

	@Override
	protected RegionFile createRegionFile(Path path) throws IOException {
		boolean isSyncChunkWrites = ((CraftServer) Bukkit.getServer()).getServer().forceSynchronousWrites();
		return new RegionFile(path, path.getParent(), RegionFileVersion.VERSION_NONE, isSyncChunkWrites);
	}

	@Override
	protected void closeRegionFile(RegionFile t) throws IOException {
		t.close();
	}

	@Override
	protected DataInputStream createInputStream(RegionFile t, ChunkPosition key) throws IOException {
		return t.getChunkDataInputStream(new ChunkPos(key.x, key.z));
	}

	@Override
	protected DataOutputStream createOutputStream(RegionFile t, ChunkPosition key) throws IOException {
		return t.getChunkDataOutputStream(new ChunkPos(key.x, key.z));
	}
}