package com.lishid.orebfuscator.cache;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

import com.lishid.orebfuscator.config.ConfigManager;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.nms.AbstractRegionFileCache;

public class CacheCleaner implements Runnable {

	private final ConfigManager configManager;

	public CacheCleaner(ConfigManager configManager) {
		this.configManager = configManager;
	}

	@Override
	public void run() {
		long deleteAfterDays = this.configManager.getConfig().getCacheConfig().deleteRegionFilesAfterAccess();
		if (!this.configManager.getConfig().isEnabled() || deleteAfterDays <= 0) {
			return;
		}

		AbstractRegionFileCache<?> regionFileCache = NmsInstance.get().getRegionFileCache();
		long deleteAfterMillis = TimeUnit.DAYS.toMillis(deleteAfterDays);

		try {
			Files.walkFileTree(this.configManager.getConfig().getCacheConfig().baseDirectory(), new SimpleFileVisitor<Path>() {

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
