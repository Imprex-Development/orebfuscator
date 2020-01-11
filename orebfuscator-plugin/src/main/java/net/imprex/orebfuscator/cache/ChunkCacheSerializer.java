package net.imprex.orebfuscator.cache;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import com.lishid.orebfuscator.NmsInstance;
import com.lishid.orebfuscator.cache.ObfuscatedDataCache;
import com.lishid.orebfuscator.nms.IChunkCache;

import net.imprex.cache.HybridCacheSerializer;
import net.imprex.orebfuscator.util.BlockCoords;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.util.EngineMode;

public class ChunkCacheSerializer implements HybridCacheSerializer<ChunkPosition, ChunkCacheEntry> {

	private static final IChunkCache REGION_CHUNK_CACHE = NmsInstance.get().createChunkCache();

	private static DataInputStream getInputStream(ChunkPosition key) throws IOException {
		return REGION_CHUNK_CACHE.getInputStream(new File(ObfuscatedDataCache.getCacheFolder(), key.getWorld()),
				key.getX(), key.getZ());
	}

	private static DataOutputStream getOutputStream(ChunkPosition key) throws IOException {
		return REGION_CHUNK_CACHE.getOutputStream(new File(ObfuscatedDataCache.getCacheFolder(), key.getWorld()),
				key.getX(), key.getZ());
	}

	@Override
	public ChunkCacheEntry read(ChunkPosition key) throws IOException {
		try (DataInputStream dataInputStream = getInputStream(key)) {
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

	@Override
	public void write(ChunkPosition key, ChunkCacheEntry value) throws IOException {
		try (DataOutputStream dataOutputStream = getOutputStream(key)) {
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
