package net.imprex.orebfuscator.nms.v1_11_R1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import net.imprex.orebfuscator.nms.AbstractChunkCache;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.minecraft.server.v1_11_R1.RegionFile;

public class ChunkCache extends AbstractChunkCache<RegionFile> {

	public ChunkCache(int maxSize) {
		super(maxSize);
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
	protected RegionFile create(Path path) throws IOException {
		return new RegionFile(path.toFile());
	}

	@Override
	protected void close(RegionFile t) throws IOException {
		t.c();
	}
}
