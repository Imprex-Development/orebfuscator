package net.imprex.orebfuscator.cache;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.obfuscation.ObfuscationResult;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ChunkSerializer {

	private static final int CACHE_VERSION = 1;

	private static DataInputStream createInputStream(ChunkPosition key) throws IOException {
		return NmsInstance.getRegionFileCache().createInputStream(key);
	}

	private static DataOutputStream createOutputStream(ChunkPosition key) throws IOException {
		return NmsInstance.getRegionFileCache().createOutputStream(key);
	}

	public static ObfuscationResult read(ChunkPosition key) throws IOException {
		try (DataInputStream dataInputStream = createInputStream(key)) {
			if (dataInputStream != null) {
				// check if cache entry has right version and if chunk is present
				if (dataInputStream.readInt() != CACHE_VERSION || !dataInputStream.readBoolean()) {
					return null;
				}

				byte[] hash = new byte[dataInputStream.readInt()];
				dataInputStream.readFully(hash);

				byte[] data = new byte[dataInputStream.readInt()];
				dataInputStream.readFully(data);

				ObfuscationResult chunkCacheEntry = new ObfuscationResult(key, hash, data);

				Collection<BlockPos> proximityBlocks = chunkCacheEntry.getProximityBlocks();
				for (int i = dataInputStream.readInt(); i > 0; i--) {
					proximityBlocks.add(BlockPos.fromLong(dataInputStream.readLong()));
				}

				Collection<BlockPos> removedEntities = chunkCacheEntry.getBlockEntities();
				for (int i = dataInputStream.readInt(); i > 0; i--) {
					removedEntities.add(BlockPos.fromLong(dataInputStream.readLong()));
				}

				return chunkCacheEntry;
			}
		} catch (IOException e) {
			throw new IOException("Unable to read chunk: " + key, e);
		}
		return null;
	}

	// TODO consider size limit for cache since RegionFile before 1.14 have a hard limit of 256 * 4kb 
	public static void write(ChunkPosition key, ObfuscationResult value) throws IOException {
		try (DataOutputStream dataOutputStream = createOutputStream(key)) {
			dataOutputStream.writeInt(CACHE_VERSION);

			if (value != null) {
				dataOutputStream.writeBoolean(true);

				byte[] hash = value.getHash();
				dataOutputStream.writeInt(hash.length);
				dataOutputStream.write(hash, 0, hash.length);

				byte[] data = value.getData();
				dataOutputStream.writeInt(data.length);
				dataOutputStream.write(data, 0, data.length);

				Collection<BlockPos> proximityBlocks = value.getProximityBlocks();
				dataOutputStream.writeInt(proximityBlocks.size());
				for (BlockPos blockPosition : proximityBlocks) {
					dataOutputStream.writeLong(blockPosition.toLong());
				}

				Collection<BlockPos> removedEntities = value.getBlockEntities();
				dataOutputStream.writeInt(removedEntities.size());
				for (BlockPos blockPosition : removedEntities) {
					dataOutputStream.writeLong(blockPosition.toLong());
				}	
			} else {
				dataOutputStream.writeBoolean(false);
			}
		}
	}

}
