package net.imprex.orebfuscator.cache;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import dev.imprex.orebfuscator.cache.AbstractRegionFileCache;
import dev.imprex.orebfuscator.config.api.CacheConfig;
import dev.imprex.orebfuscator.logging.OfcLogger;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorNms;

public class CacheFileCleanupTask implements Runnable {

  private final CacheConfig cacheConfig;

  private int deleteCount = 0;

  public CacheFileCleanupTask(Orebfuscator orebfuscator) {
    this.cacheConfig = orebfuscator.getOrebfuscatorConfig().cache();
  }

  @Override
  public void run() {
    if (Files.notExists(this.cacheConfig.baseDirectory())) {
      OfcLogger.debug("Skipping CacheFileCleanupTask as the cache directory doesn't exist.");
      return;
    }

    long deleteAfterMillis = this.cacheConfig.deleteRegionFilesAfterAccess();
    AbstractRegionFileCache<?> regionFileCache = OrebfuscatorNms.getRegionFileCache();

    this.deleteCount = 0;

    try {
      Files.walkFileTree(this.cacheConfig.baseDirectory(), new SimpleFileVisitor<Path>() {

        @Override
        public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) throws IOException {
          if (System.currentTimeMillis() - attributes.lastAccessTime().toMillis() > deleteAfterMillis) {
            regionFileCache.close(path);
            Files.delete(path);

            CacheFileCleanupTask.this.deleteCount++;
            OfcLogger.debug("deleted cache file: " + path);
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      e.printStackTrace();
    }

    if (this.deleteCount > 0) {
      OfcLogger.info(String.format("CacheFileCleanupTask successfully deleted %d cache file(s)", this.deleteCount));
    }
  }
}
