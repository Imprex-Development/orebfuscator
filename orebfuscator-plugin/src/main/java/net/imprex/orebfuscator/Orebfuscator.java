package net.imprex.orebfuscator;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.imprex.orebfuscator.cache.ChunkCache;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.obfuscation.ObfuscationListener;
import net.imprex.orebfuscator.obfuscation.Obfuscator;
import net.imprex.orebfuscator.obfuscation.PacketListener;
import net.imprex.orebfuscator.proximityhider.ProximityHider;
import net.imprex.orebfuscator.proximityhider.ProximityListener;
import net.imprex.orebfuscator.proximityhider.ProximityPacketListener;
import net.imprex.orebfuscator.util.OFCLogger;

public class Orebfuscator extends JavaPlugin implements Listener {

	public static final ThreadGroup THREAD_GROUP = new ThreadGroup("ofc");

	private final Thread mainThread = Thread.currentThread();

	private OrebfuscatorConfig config;
	private ChunkCache chunkCache;
	private Obfuscator obfuscator;
	private ProximityHider proximityHider;
	private PacketListener packetListener;
	private ProximityPacketListener proximityPacketListener;

	@Override
	public void onEnable() {
		try {
			// Check if protocolLib is enabled
			if (this.getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
				OFCLogger.info("[OFC] ProtocolLib is not found! Plugin cannot be enabled.");
				return;
			}

			// Load configurations
			this.config = new OrebfuscatorConfig(this);

			// Load chunk cache	
			this.chunkCache = new ChunkCache(this);

			// Load obfuscater
			this.obfuscator = new Obfuscator(this);
			this.getServer().getPluginManager().registerEvents(new ObfuscationListener(this), this);

			// Load proximity hider
			this.proximityHider = new ProximityHider(this);
			if (this.config.proximityEnabled()) {
				this.proximityHider.start();

				this.proximityPacketListener = new ProximityPacketListener(this);

				this.getServer().getPluginManager().registerEvents(new ProximityListener(this), this);
			}

			// Load packet listener
			this.packetListener = new PacketListener(this);

		} catch(Exception e) {
			OFCLogger.log(Level.SEVERE, "An error occurred while enabling plugin");
			OFCLogger.err(e);

			this.getServer().getPluginManager().registerEvent(PluginEnableEvent.class, this, EventPriority.NORMAL, this::onEnableFailed, this);
		}
	}

	@Override
	public void onDisable() {
		this.chunkCache.close();
		this.config.store();

		this.packetListener.unregister();

		if (this.config.proximityEnabled()) {
			this.proximityPacketListener.unregister();
			this.proximityHider.close();
		}

		this.getServer().getScheduler().cancelTasks(this);


		NmsInstance.close();
		this.config = null;
	}

	public void onEnableFailed(Listener listener, Event event) {
		PluginEnableEvent enableEvent = (PluginEnableEvent) event;

		if (enableEvent.getPlugin() == this) {
			HandlerList.unregisterAll(listener);
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}

	public boolean isMainThread() {
		return Thread.currentThread() == this.mainThread;
	}

	public OrebfuscatorConfig getOrebfuscatorConfig() {
		return this.config;
	}

	public ChunkCache getChunkCache() {
		return this.chunkCache;
	}

	public Obfuscator getObfuscator() {
		return this.obfuscator;
	}

	public ProximityHider getProximityHider() {
		return this.proximityHider;
	}

	public PacketListener getPacketListener() {
		return this.packetListener;
	}

	public ProximityPacketListener getProximityPacketListener() {
		return this.proximityPacketListener;
	}
}