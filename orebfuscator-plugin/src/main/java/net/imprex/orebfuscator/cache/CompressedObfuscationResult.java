package net.imprex.orebfuscator.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.obfuscation.ObfuscationRequest;
import net.imprex.orebfuscator.obfuscation.ObfuscationResult;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

public class CompressedObfuscationResult {

  public static CompressedObfuscationResult create(ObfuscationResult result) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    try (
        LZ4BlockOutputStream lz4BlockOutputStream = new LZ4BlockOutputStream(byteArrayOutputStream);
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
      new IOException("Unable to compress chunk: " + result.getPosition(), e).printStackTrace();
      return null;
    }

    return new CompressedObfuscationResult(result.getPosition(), byteArrayOutputStream.toByteArray());
  }

  private final ChunkPosition position;
  private final byte[] compressedData;

  public CompressedObfuscationResult(ChunkPosition position, byte[] data) {
    this.position = position;
    this.compressedData = data;
  }

  public byte[] compressedData() {
    return compressedData;
  }

  public int estimatedSize() {
    return 128 + this.compressedData.length;
  }

  public boolean isValid(ObfuscationRequest request) {
    try {
      return request != null && Arrays.equals(this.compressedData, 0, ObfuscationRequest.HASH_LENGTH,
          request.getChunkHash(), 0, ObfuscationRequest.HASH_LENGTH);
    } catch (Exception e) {
      throw new RuntimeException("unable to validate", e);
    }
  }

  public Optional<ObfuscationResult> toResult() {
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

      return Optional.of(result);
    } catch (Exception e) {
      new IOException("Unable to decompress chunk: " + this.position, e).printStackTrace();
      return Optional.empty();
    }
  }
}
