package net.imprex.orebfuscator.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.joml.Math;

import net.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import net.imprex.orebfuscator.obfuscation.ObfuscationResult;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;
import net.jpountz.lz4.LZ4Factory;

public class CompressedObfuscationResult {
	
	private static final AtomicInteger COUNT = new AtomicInteger();
	private static final AtomicLong TIME = new AtomicLong();

	public static CompressedObfuscationResult create(ObfuscationResult result) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		
		long time = System.nanoTime();
		try (
			 LZ4BlockOutputStream lz4BlockOutputStream = new LZ4BlockOutputStream(byteArrayOutputStream, 1 << 16, LZ4Factory.fastestInstance().highCompressor(17));
			 DataOutputStream dataOutputStream = new DataOutputStream(lz4BlockOutputStream)) {

			byteArrayOutputStream.write(result.getHash());

			byte[] data = result.getData();
			dataOutputStream.writeInt(data.length);
			dataOutputStream.write(data, 0, data.length);

			Collection<BlockPos> proximityBlocks = result.getProximityBlocks();
			dataOutputStream.writeInt(proximityBlocks.size());
			for (BlockPos blockPosition : proximityBlocks) {
				dataOutputStream.writeInt(blockPosition.toSectionPos());
			}

			Collection<BlockPos> removedEntities = result.getBlockEntities();
			dataOutputStream.writeInt(removedEntities.size());
			for (BlockPos blockPosition : removedEntities) {
				dataOutputStream.writeInt(blockPosition.toSectionPos());
			}
		} catch (Exception e) {
			throw new RuntimeException("unable to compress", e);
		}
		
		long usedTime = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - time);
		long total = TIME.addAndGet(usedTime);
		
		if (COUNT.getAndIncrement() % 100 == 0) {
			System.out.println(String.format("tpc: %sµs, time: %sµs, count; %s", Math.round((total / (float)COUNT.get()) * 100) / 100, usedTime, COUNT.get()));
		}

		return new CompressedObfuscationResult(result.getPosition(), byteArrayOutputStream.toByteArray());
	}

	private final ChunkPosition position;
	private final byte[] compressedData;

	public CompressedObfuscationResult(ChunkPosition position, byte[] data) {
		this.position = position;
		this.compressedData = data;
	}

	public byte[] getCompressedData() {
		return compressedData;
	}

	public boolean isValid(ObfuscationRequest request) {
		try {
			return request != null && Arrays.equals(this.compressedData, 0, ObfuscationRequest.HASH_LENGTH,
					request.getChunkHash(), 0, ObfuscationRequest.HASH_LENGTH);
		} catch (Exception e) {
			throw new RuntimeException("unable to validate", e);
		}
	}

	public ObfuscationResult toResult() {
		try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(this.compressedData);
				LZ4BlockInputStream lz4BlockInputStream = new LZ4BlockInputStream(byteArrayInputStream);
				DataInputStream dataInputStream = new DataInputStream(lz4BlockInputStream)) {

			byte[] hash = Arrays.copyOf(this.compressedData, ObfuscationRequest.HASH_LENGTH);
			byteArrayInputStream.skip(ObfuscationRequest.HASH_LENGTH);

			byte[] data = new byte[dataInputStream.readInt()];
			dataInputStream.readFully(data);

			ObfuscationResult result = new ObfuscationResult(this.position, hash, data);

			int x = this.position.x << 4;
			int z = this.position.z << 4;

			Collection<BlockPos> proximityBlocks = result.getProximityBlocks();
			for (int i = dataInputStream.readInt(); i > 0; i--) {
				proximityBlocks.add(BlockPos.fromSectionPos(x, z, dataInputStream.readInt()));
			}

			Collection<BlockPos> removedEntities = result.getBlockEntities();
			for (int i = dataInputStream.readInt(); i > 0; i--) {
				removedEntities.add(BlockPos.fromSectionPos(x, z, dataInputStream.readInt()));
			}
			
			return result;
		} catch (Exception e) {
			throw new RuntimeException("unable to decompress", e);
		}
	}
}
