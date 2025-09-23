package dev.imprex.orebfuscator.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.imprex.orebfuscator.obfuscation.CacheRequest;
import dev.imprex.orebfuscator.obfuscation.CacheResponse;
import dev.imprex.orebfuscator.obfuscation.ObfuscationResponse;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.ChunkCacheKey;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

public record CacheChunkEntry(ChunkCacheKey key, byte[] compressedData) {

  public static @Nullable CacheChunkEntry create(@NotNull CacheRequest request, @NotNull ObfuscationResponse response) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    try (LZ4BlockOutputStream lz4BlockOutputStream = new LZ4BlockOutputStream(byteArrayOutputStream);
        DataOutputStream dataOutputStream = new DataOutputStream(lz4BlockOutputStream)) {

      byteArrayOutputStream.write(request.hash());

      byte[] data = response.data();
      dataOutputStream.writeInt(data.length);
      dataOutputStream.write(data, 0, data.length);

      Collection<BlockPos> proximityBlocks = response.proximityBlocks();
      dataOutputStream.writeInt(proximityBlocks.size());
      for (BlockPos blockPosition : proximityBlocks) {
        dataOutputStream.writeInt(blockPosition.toSectionPos());
      }

      Collection<BlockPos> blockEntities = response.blockEntities();
      dataOutputStream.writeInt(blockEntities.size());
      for (BlockPos blockPosition : blockEntities) {
        dataOutputStream.writeInt(blockPosition.toSectionPos());
      }
    } catch (Exception e) {
      new IOException("Unable to compress chunk: " + request.cacheKey(), e).printStackTrace();
      return null;
    }

    return new CacheChunkEntry(request.cacheKey(), byteArrayOutputStream.toByteArray());
  }

  public int estimatedSize() {
    return 64 + key.world().length() + compressedData.length;
  }

  public boolean isValid(@Nullable CacheRequest request) {
    try {
      return request != null
          && Arrays.equals(compressedData, 0, CacheRequest.HASH_LENGTH, request.hash(), 0, CacheRequest.HASH_LENGTH);
    } catch (Exception e) {
      throw new RuntimeException("Unable to validate cache entry hash", e);
    }
  }

  public @NotNull Optional<CacheResponse> toResult() {
    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedData);
        LZ4BlockInputStream lz4BlockInputStream = new LZ4BlockInputStream(byteArrayInputStream);
        DataInputStream dataInputStream = new DataInputStream(lz4BlockInputStream)) {

      byte[] hash = Arrays.copyOf(compressedData, CacheRequest.HASH_LENGTH);
      byteArrayInputStream.skip(CacheRequest.HASH_LENGTH);

      byte[] data = new byte[dataInputStream.readInt()];
      dataInputStream.readFully(data);

      int x = key.x() << 4;
      int z = key.z() << 4;

      var proximityBlocks = new ArrayList<BlockPos>();
      for (int i = dataInputStream.readInt(); i > 0; i--) {
        proximityBlocks.add(BlockPos.fromSectionPos(x, z, dataInputStream.readInt()));
      }

      var blockEntities = new HashSet<BlockPos>();
      for (int i = dataInputStream.readInt(); i > 0; i--) {
        blockEntities.add(BlockPos.fromSectionPos(x, z, dataInputStream.readInt()));
      }

      var response = new ObfuscationResponse(data, blockEntities, proximityBlocks);
      return Optional.of(new CacheResponse(response, key, hash));
    } catch (Exception e) {
      new IOException("Unable to decompress chunk: " + key, e).printStackTrace();
      return Optional.empty();
    }
  }
}
