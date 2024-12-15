package net.imprex.orebfuscator;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import net.imprex.orebfuscator.api.OrebfuscatorService;
import net.imprex.orebfuscator.cache.ObfuscationCache;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.obfuscation.ObfuscationSystem;
import net.imprex.orebfuscator.player.OrebfuscatorPlayerMap;
import net.imprex.orebfuscator.proximity.ProximityDirectorThread;
import net.imprex.orebfuscator.proximity.ProximityPacketListener;
import net.imprex.orebfuscator.util.HeightAccessor;
import net.imprex.orebfuscator.util.MinecraftVersion;
import net.imprex.orebfuscator.util.OFCLogger;

public class Orebfuscator extends JavaPlugin implements Listener {

	public static final ThreadGroup THREAD_GROUP = new ThreadGroup("orebfuscator");

	private final Thread mainThread = Thread.currentThread();

	private OrebfuscatorStatistics statistics;
	private OrebfuscatorConfig config;
	private OrebfuscatorPlayerMap playerMap;
	private UpdateSystem updateSystem;
	private ObfuscationCache obfuscationCache;
	private ObfuscationSystem obfuscationSystem;
	private ProximityDirectorThread proximityThread;
	private ProximityPacketListener proximityPacketListener;

	@Override
	public void onLoad() {
		OFCLogger.LOGGER = getLogger();
	}

	@Override
	public void onEnable() {
		try {
			// GZIPOutputStream
			// Check for valid minecraft version
			if (MinecraftVersion.isBelow("1.16")) {
				throw new RuntimeException("Orebfuscator only supports minecraft 1.16 and above");
			}
			
			// Check if protocolLib is enabled
			Plugin protocolLib = getServer().getPluginManager().getPlugin("ProtocolLib");
			if (protocolLib == null || !protocolLib.isEnabled()) {
				throw new RuntimeException("ProtocolLib can't be found or is disabled! Orebfuscator can't be enabled.");
			}

			this.statistics = new OrebfuscatorStatistics();

			// Load configurations
			this.config = new OrebfuscatorConfig(this);

			this.playerMap = new OrebfuscatorPlayerMap(this);

			// register cleanup listener
			HeightAccessor.registerListener(this);

			// Initialize metrics
			new MetricsSystem(this);

			// initialize update system and check for updates
			this.updateSystem = new UpdateSystem(this);

			// Load chunk cache
			this.obfuscationCache = new ObfuscationCache(this);

			// Load obfuscater
			this.obfuscationSystem = new ObfuscationSystem(this);

			// Load proximity hider
			this.proximityThread = new ProximityDirectorThread(this);
			if (this.config.proximityEnabled()) {
				this.proximityThread.start();

				this.proximityPacketListener = new ProximityPacketListener(this);
			}

			// Load packet listener
			this.obfuscationSystem.registerChunkListener();

			// Store formatted config
			this.config.store();
			
			// initialize service
			Bukkit.getServicesManager().register(
					OrebfuscatorService.class,
					new DefaultOrebfuscatorService(this),
					this, ServicePriority.Normal);

			// add commands
			getCommand("orebfuscator").setExecutor(new OrebfuscatorCommand(this));
		} catch (Exception e) {
			OFCLogger.error("An error occurred while enabling plugin", e);

			this.getServer().getPluginManager().registerEvent(PluginEnableEvent.class, this, EventPriority.NORMAL,
					this::onEnableFailed, this);
		}
	}

	@Override
	public void onDisable() {
		if (this.obfuscationCache != null) {
			this.obfuscationCache.close();
		}

		if (this.obfuscationSystem != null) {
			this.obfuscationSystem.shutdown();
		}

		if (this.config != null && this.config.proximityEnabled() && this.proximityPacketListener != null && this.proximityThread != null) {
			this.proximityPacketListener.unregister();
			this.proximityThread.close();
		}

		OrebfuscatorCompatibility.close();
		OrebfuscatorNms.close();
		
		this.config = null;
	}

	public void onEnableFailed(Listener listener, Event event) {
		PluginEnableEvent enableEvent = (PluginEnableEvent) event;

		if (enableEvent.getPlugin() == this) {
			HandlerList.unregisterAll(listener);
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}

	public boolean isGameThread() {
		return Thread.currentThread() == this.mainThread;
	}

	public OrebfuscatorStatistics getStatistics() {
		return statistics;
	}

	public OrebfuscatorConfig getOrebfuscatorConfig() {
		return this.config;
	}

	public OrebfuscatorPlayerMap getPlayerMap() {
		return playerMap;
	}

	public UpdateSystem getUpdateSystem() {
		return updateSystem;
	}

	public ObfuscationCache getObfuscationCache() {
		return this.obfuscationCache;
	}

	public ObfuscationSystem getObfuscationSystem() {
		return obfuscationSystem;
	}

	public ProximityPacketListener getProximityPacketListener() {
		return this.proximityPacketListener;
	}
}