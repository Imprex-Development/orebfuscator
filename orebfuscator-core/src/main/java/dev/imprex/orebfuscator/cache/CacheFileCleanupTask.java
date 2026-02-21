package dev.imprex.orebfuscator.cache;

import dev.imprex.orebfuscator.config.api.CacheConfig;
import dev.imprex.orebfuscator.config.api.Config;
import dev.imprex.orebfuscator.logging.OfcLogger;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public class CacheFileCleanupTask implements Runnable {

  private final CacheConfig cacheConfig;
  private final AbstractRegionFileCache<?> regionFileCache;

  private int deleteCount = 0;

  public CacheFileCleanupTask(Config config, AbstractRegionFileCache<?> regionFileCache) {
    this.cacheConfig = config.cache();
    this.regionFileCache = regionFileCache;
  }

  @Override
  public void run() {
    if (Files.notExists(this.cacheConfig.baseDirectory())) {
      OfcLogger.debug("Skipping CacheFileCleanupTask as the cache directory doesn't exist.");
      return;
    }

    long deleteAfterMillis = this.cacheConfig.deleteRegionFilesAfterAccess();

    this.deleteCount = 0;

    try {
      Path basePath = this.cacheConfig.baseDirectory();
      Files.walkFileTree(basePath, new SimpleFileVisitor<>() {

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) throws IOException {
          if (System.currentTimeMillis() - attributes.lastAccessTime().toMillis() > deleteAfterMillis) {
            regionFileCache.close(path);
            Files.delete(path);

            CacheFileCleanupTask.this.deleteCount++;
            OfcLogger.debug("Deleted cache file: " + path);
          }
          return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) throws IOException {
          try {
            if (!basePath.equals(dir)) {
              Files.delete(dir);
            }
          } catch (NoSuchFileException | DirectoryNotEmptyException e) {
            // NOOP; we don't care
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      OfcLogger.error(e);
    }

    if (this.deleteCount > 0) {
      OfcLogger.info(String.format("CacheFileCleanupTask successfully deleted %d cache file(s)", this.deleteCount));
    }
  }
}
