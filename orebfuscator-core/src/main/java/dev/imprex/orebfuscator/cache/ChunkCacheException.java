package dev.imprex.orebfuscator.cache;

public class ChunkCacheException extends RuntimeException {

  public ChunkCacheException(String message) {
    super(message);
  }

  public ChunkCacheException(String message, Throwable cause) {
    super(message, cause);
  }
}
