package net.imprex.orebfuscator.cache;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.config.CacheConfig;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;
import net.imprex.orebfuscator.util.OFCLogger;

public class CacheFileCleanupTask implements Runnable {

	private final CacheConfig cacheConfig;

	private int deleteCount = 0;

	public CacheFileCleanupTask(Orebfuscator orebfuscator) {
		this.cacheConfig = orebfuscator.getOrebfuscatorConfig().cache();
	}

	@Override
	public void run() {
		long deleteAfterMillis = this.cacheConfig.deleteRegionFilesAfterAccess();
		AbstractRegionFileCache<?> regionFileCache = NmsInstance.getRegionFileCache();

		this.deleteCount = 0;

		try {
			Files.walkFileTree(this.cacheConfig.baseDirectory(), new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) throws IOException {
					if (System.currentTimeMillis() - attributes.lastAccessTime().toMillis() > deleteAfterMillis) {
						regionFileCache.close(path);
						Files.delete(path);
						
						CacheFileCleanupTask.this.deleteCount++;
						OFCLogger.debug("deleted cache file: " + path);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (this.deleteCount > 0) {
			OFCLogger.info(String.format("CacheFileCleanupTask successfully deleted %d cache file(s)", this.deleteCount));
		}
	}
}
