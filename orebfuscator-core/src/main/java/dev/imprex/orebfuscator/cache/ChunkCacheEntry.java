package dev.imprex.orebfuscator.cache;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.obfuscation.ObfuscationResponse;
import dev.imprex.orebfuscator.player.ProximityBlock;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.ChunkCacheKey;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.jpountz.lz4.LZ4BlockOutputStream;

@NullMarked
public record ChunkCacheEntry(ChunkCacheKey key, byte[] compressedData) {

  public static ChunkCacheEntry create(CacheRequest request, ObfuscationResponse response) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    try (LZ4BlockOutputStream lz4BlockOutputStream = new LZ4BlockOutputStream(byteArrayOutputStream);
        DataOutputStream dataOutputStream = new DataOutputStream(lz4BlockOutputStream)) {

      byteArrayOutputStream.write(request.hash());

      byte[] data = response.data();
      dataOutputStream.writeInt(data.length);
      dataOutputStream.write(data, 0, data.length);

      Collection<ProximityBlock> proximityBlocks = response.proximityBlocks();
      dataOutputStream.writeInt(proximityBlocks.size());
      for (ProximityBlock blockPosition : proximityBlocks) {
        dataOutputStream.writeInt(blockPosition.blockPos().toSectionPos());
        dataOutputStream.writeByte(blockPosition.flags());
      }

      Collection<BlockPos> blockEntities = response.blockEntities();
      dataOutputStream.writeInt(blockEntities.size());
      for (BlockPos blockPosition : blockEntities) {
        dataOutputStream.writeInt(blockPosition.toSectionPos());
      }
    } catch (Exception e) {
      throw new ChunkCacheException("Unable to compress chunk: " + request.cacheKey(), e);
    }

    return new ChunkCacheEntry(request.cacheKey(), byteArrayOutputStream.toByteArray());
  }

  public int estimatedSize() {
    return 64 + key.world().length() + compressedData.length;
  }

  public boolean isValid(CacheRequest request) {
    return Arrays.equals(compressedData, 0, CacheRequest.HASH_LENGTH, request.hash(), 0, CacheRequest.HASH_LENGTH);
  }

  public ObfuscationResponse toResult() {
    try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(compressedData);
        LZ4BlockInputStream lz4BlockInputStream = LZ4BlockInputStream.newBuilder().build(byteArrayInputStream);
        DataInputStream dataInputStream = new DataInputStream(lz4BlockInputStream)) {

      byteArrayInputStream.skip(CacheRequest.HASH_LENGTH);

      byte[] data = new byte[dataInputStream.readInt()];
      dataInputStream.readFully(data);

      int x = key.x() << 4;
      int z = key.z() << 4;

      var proximityBlocks = new ArrayList<ProximityBlock>();
      for (int i = dataInputStream.readInt(); i > 0; i--) {
        var blockPos = BlockPos.fromSectionPos(x, z, dataInputStream.readInt());
        var flags = dataInputStream.readByte();
        proximityBlocks.add(new ProximityBlock(blockPos, flags));
      }

      var blockEntities = new HashSet<BlockPos>();
      for (int i = dataInputStream.readInt(); i > 0; i--) {
        blockEntities.add(BlockPos.fromSectionPos(x, z, dataInputStream.readInt()));
      }

      return new ObfuscationResponse(data, blockEntities, proximityBlocks);
    } catch (Exception e) {
      throw new ChunkCacheException("Unable to decompress chunk: " + key, e);
    }
  }
}
