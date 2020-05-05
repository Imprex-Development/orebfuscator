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

public class CacheCleanTask implements Runnable {

	private final CacheConfig cacheConfig;

	public CacheCleanTask(Orebfuscator orebfuscator) {
		this.cacheConfig = orebfuscator.getOrebfuscatorConfig().cache();
	}

	@Override
	public void run() {
		long deleteAfterMillis = this.cacheConfig.deleteRegionFilesAfterAccess();
		if (!this.cacheConfig.enabled() || deleteAfterMillis <= 0) {
			return;
		}

		AbstractRegionFileCache<?> regionFileCache = NmsInstance.get().getRegionFileCache();
		try {
			Files.walkFileTree(this.cacheConfig.baseDirectory(), new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult visitFile(Path path, BasicFileAttributes attributes) throws IOException {
					if (System.currentTimeMillis() - attributes.lastAccessTime().toMillis() > deleteAfterMillis) {
						regionFileCache.close(path);
						Files.delete(path);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
