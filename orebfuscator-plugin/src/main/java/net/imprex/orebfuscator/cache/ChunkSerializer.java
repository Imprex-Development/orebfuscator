package net.imprex.orebfuscator.cache;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import dev.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.OrebfuscatorNms;

public class ChunkSerializer {

  private static final int CACHE_VERSION = 2;

  private static DataInputStream createInputStream(ChunkPosition key) throws IOException {
    return OrebfuscatorNms.getRegionFileCache().createInputStream(key);
  }

  private static DataOutputStream createOutputStream(ChunkPosition key) throws IOException {
    return OrebfuscatorNms.getRegionFileCache().createOutputStream(key);
  }

  public static CompressedObfuscationResult read(ChunkPosition key) throws IOException {
    try (DataInputStream dataInputStream = createInputStream(key)) {
      if (dataInputStream != null) {
        // check if cache entry has right version and if chunk is present
        if (dataInputStream.readInt() != CACHE_VERSION || !dataInputStream.readBoolean()) {
          return null;
        }

        byte[] compressedData = new byte[dataInputStream.readInt()];
        dataInputStream.readFully(compressedData);

        return new CompressedObfuscationResult(key, compressedData);
      }
    } catch (IOException e) {
      throw new IOException("Unable to read chunk: " + key, e);
    }

    return null;
  }

  public static void write(ChunkPosition key, CompressedObfuscationResult value) throws IOException {
    try (DataOutputStream dataOutputStream = createOutputStream(key)) {
      dataOutputStream.writeInt(CACHE_VERSION);

      if (value != null) {
        dataOutputStream.writeBoolean(true);

        byte[] compressedData = value.compressedData();
        dataOutputStream.writeInt(compressedData.length);
        dataOutputStream.write(compressedData);
      } else {
        dataOutputStream.writeBoolean(false);
      }
    } catch (IOException e) {
      throw new IOException("Unable to write chunk: " + key, e);
    }
  }

}
