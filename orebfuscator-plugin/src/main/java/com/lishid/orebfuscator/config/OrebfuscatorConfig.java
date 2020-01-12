/**
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.config;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.lishid.orebfuscator.Orebfuscator;

import net.imprex.orebfuscator.config.CacheConfig;

public class OrebfuscatorConfig {
	// Caching
	private boolean useCache;
	private int maxLoadedCacheFiles;
	private String cacheLocation;
	private int deleteCacheFilesAfterDays;
	private CacheConfig cacheConfig = new CacheConfig() {
		
		@Override
		public int maximumSize() {
			return 2048;
		}
		
		@Override
		public int maximumOpenRegionFiles() {
			return 256;
		}
		
		@Override
		public long expireAfterAccess() {
			return TimeUnit.DAYS.toMillis(2);
		}
		
		@Override
		public boolean enabled() {
			return true;
		}
		
		@Override
		public Path baseDirectory() {
			return Bukkit.getWorldContainer().toPath().resolve("orebfuscator_cache");
		}
	};

	// Main engine config
	private boolean enabled;
	private boolean updateOnDamage;
	private int engineMode;
	private int initialRadius;
	private int updateRadius;
	private boolean noObfuscationForMetadata;
	private String noObfuscationForMetadataTagName;
	private boolean noObfuscationForOps;
	private boolean noObfuscationForPermission;
	private boolean loginNotification;

	private byte[] transparentBlocks;

	private Map<String, WorldConfig> worlds;

	private boolean proximityHiderEnabled;

	private static final int antiHitHackDecrementFactor = 1000;
	private static final int antiHitHackMaxViolation = 15;
	private static final int proximityHiderRate = 500;
	private static final long cacheCleanRate = 60 * 60 * 20;// once per hour

	public CacheConfig getCacheConfig() {
		return this.cacheConfig;
	}

	public boolean isUseCache() {
		return this.useCache;
	}

	public void setUseCache(boolean value) {
		this.useCache = value;
	}

	public int getMaxLoadedCacheFiles() {
		return this.maxLoadedCacheFiles;
	}

	public void setMaxLoadedCacheFiles(int value) {
		this.maxLoadedCacheFiles = value;
	}

	public String getCacheLocation() {
		return this.cacheLocation;
	}

	public void setCacheLocation(String value) {
		this.cacheLocation = value;
	}

	public int getDeleteCacheFilesAfterDays() {
		return this.deleteCacheFilesAfterDays;
	}

	public void setDeleteCacheFilesAfterDays(int value) {
		this.deleteCacheFilesAfterDays = value;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean value) {
		this.enabled = value;
	}

	public boolean isUpdateOnDamage() {
		return this.updateOnDamage;
	}

	public void setUpdateOnDamage(boolean value) {
		this.updateOnDamage = value;
	}

	public int getEngineMode() {
		return this.engineMode;
	}

	public void setEngineMode(int value) {
		this.engineMode = value;
	}

	public int getInitialRadius() {
		return this.initialRadius;
	}

	public void setInitialRadius(int value) {
		this.initialRadius = value;
	}

	public int getUpdateRadius() {
		return this.updateRadius;
	}

	public void setUpdateRadius(int value) {
		this.updateRadius = value;
	}

	public boolean isNoObfuscationForMetadata() {
		return this.noObfuscationForMetadata;
	}

	public void setNoObfuscationForMetadata(boolean value) {
		this.noObfuscationForMetadata = value;
	}

	public String getNoObfuscationForMetadataTagName() {
		return this.noObfuscationForMetadataTagName;
	}

	public void setNoObfuscationForMetadataTagName(String value) {
		this.noObfuscationForMetadataTagName = value;
	}

	public boolean isNoObfuscationForOps() {
		return this.noObfuscationForOps;
	}

	public void setNoObfuscationForOps(boolean value) {
		this.noObfuscationForOps = value;
	}

	public boolean isNoObfuscationForPermission() {
		return this.noObfuscationForPermission;
	}

	public void setNoObfuscationForPermission(boolean value) {
		this.noObfuscationForPermission = value;
	}

	public boolean isLoginNotification() {
		return this.loginNotification;
	}

	public void setLoginNotification(boolean value) {
		this.loginNotification = value;
	}

	public void setTransparentBlocks(byte[] transparentBlocks) {
		this.transparentBlocks = transparentBlocks;
	}

	public String getWorldNames() {
		String worldNames = "";

		for (WorldConfig world : this.worlds.values()) {
			if (worldNames.length() > 0) {
				worldNames += ", ";
			}

			worldNames += world.getName();
		}

		return worldNames;
	}

	public WorldConfig getWorld(String name) {
		return this.worlds.get(name.toLowerCase());
	}

	public void setWorlds(Map<String, WorldConfig> value) {
		this.worlds = value;
	}

	public boolean isProximityHiderEnabled() {
		return this.proximityHiderEnabled;
	}

	public void setProximityHiderEnabled() {
		if (!this.proximityHiderEnabled) {
			for (WorldConfig world : this.worlds.values()) {
				if (world.getProximityHiderConfig().isEnabled() != null
						&& world.getProximityHiderConfig().isEnabled()) {
					this.proximityHiderEnabled = true;
					break;
				}
			}
		}
	}

	public int getAntiHitHackDecrementFactor() {
		return antiHitHackDecrementFactor;
	}

	public int getAntiHitHackMaxViolation() {
		return antiHitHackMaxViolation;
	}

	public int getProximityHiderRate() {
		return proximityHiderRate;
	}

	public long getCacheCleanRate() {
		return cacheCleanRate;
	}

	// Helper methods

	public boolean isBlockTransparent(int id) {
		return this.transparentBlocks[id] == 1;
	}

	public boolean obfuscateForPlayer(Player player) {
		return !(this.playerBypassOp(player) || this.playerBypassPerms(player) || this.playerBypassMetadata(player));
	}

	public boolean playerBypassOp(Player player) {
		boolean ret = false;
		try {
			ret = this.noObfuscationForOps && player.isOp();
		} catch (Exception e) {
			Orebfuscator
					.log("Error while obtaining Operator status for player" + player.getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		return ret;
	}

	public boolean playerBypassPerms(Player player) {
		boolean ret = false;
		try {
			ret = this.noObfuscationForPermission && player.hasPermission("Orebfuscator.deobfuscate");
		} catch (Exception e) {
			Orebfuscator.log("Error while obtaining permissions for player" + player.getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		return ret;
	}

	public boolean playerBypassMetadata(Player player) {
		boolean ret = false;
		try {
			ret = this.noObfuscationForMetadata && player.hasMetadata(this.noObfuscationForMetadataTagName)
					&& player.getMetadata(this.noObfuscationForMetadataTagName).get(0).asBoolean();
		} catch (Exception e) {
			Orebfuscator.log("Error while obtaining metadata for player" + player.getName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		return ret;
	}
}