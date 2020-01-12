package net.imprex.orebfuscator.cache;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.util.BlockCoords;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.util.EngineMode;

public class ChunkCacheSerializer {

	private static final AbstractRegionFileCache<?> REGION_CHUNK_CACHE = NmsInstance.get().getRegionFileCache();

	private final CacheConfig cacheConfig;

	public ChunkCacheSerializer(CacheConfig cacheConfig) {
		this.cacheConfig = cacheConfig;
	}

	private final Path getPath(ChunkPosition key) {
		return this.cacheConfig.baseDirectory().resolve(key.getWorld())
				.resolve("r." + (key.getX() >> 5) + "." + (key.getZ() >> 5) + ".mca");
	}

	private DataInputStream getInputStream(ChunkPosition key) throws IOException {
		return REGION_CHUNK_CACHE.getInputStream(getPath(key), key);
	}

	private DataOutputStream getOutputStream(ChunkPosition key) throws IOException {
		return REGION_CHUNK_CACHE.getOutputStream(getPath(key), key);
	}

	public void closeRegionFileCache() {
		REGION_CHUNK_CACHE.clear();
	}

	public ChunkCacheEntry read(ChunkPosition key) throws IOException {
		try (DataInputStream dataInputStream = this.getInputStream(key)) {
			if (dataInputStream != null) {
				long hash = dataInputStream.readLong();
				EngineMode engineMode = EngineMode.values()[dataInputStream.readByte()];

				byte[] data = new byte[dataInputStream.readInt()];
				dataInputStream.readFully(data);

				ChunkCacheEntry chunkCacheEntry = new ChunkCacheEntry(hash, engineMode, data);

				List<BlockCoords> proximityBlocks = chunkCacheEntry.getProximityBlocks();
				for (int i = dataInputStream.readInt(); i > 0; i--) {
					proximityBlocks.add(BlockCoords.fromLong(dataInputStream.readLong()));
				}

				List<BlockCoords> removedEntities = chunkCacheEntry.getRemovedEntities();
				for (int i = dataInputStream.readInt(); i > 0; i--) {
					removedEntities.add(BlockCoords.fromLong(dataInputStream.readLong()));
				}

				return chunkCacheEntry;
			}
		} catch (IOException e) {
			throw new IOException("Unable to read chunk: " + key, e);
		}
		return null;
	}

	public void write(ChunkPosition key, ChunkCacheEntry value) throws IOException {
		try (DataOutputStream dataOutputStream = this.getOutputStream(key)) {
			dataOutputStream.writeLong(value.getHash());
			dataOutputStream.writeByte(value.getEngineMode().ordinal());

			byte[] data = value.getData();
			dataOutputStream.writeInt(data.length);
			dataOutputStream.write(data, 0, data.length);

			List<BlockCoords> proximityBlocks = value.getProximityBlocks();
			dataOutputStream.writeInt(proximityBlocks.size());
			for (BlockCoords blockPosition : proximityBlocks) {
				dataOutputStream.writeLong(blockPosition.toLong());
			}

			List<BlockCoords> removedEntities = value.getRemovedEntities();
			dataOutputStream.writeInt(removedEntities.size());
			for (BlockCoords blockPosition : removedEntities) {
				dataOutputStream.writeLong(blockPosition.toLong());
			}
		}
	}

}
