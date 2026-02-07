package net.imprex.orebfuscator;

import dev.imprex.orebfuscator.UpdateSystem;
import dev.imprex.orebfuscator.cache.AbstractRegionFileCache;
import dev.imprex.orebfuscator.cache.ObfuscationCache;
import dev.imprex.orebfuscator.chunk.ChunkFactory;
import dev.imprex.orebfuscator.config.OrebfuscatorConfig;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.interop.PlayerAccessor;
import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.obfuscation.ObfuscationPipeline;
import dev.imprex.orebfuscator.obfuscation.ObfuscationProcessor;
import dev.imprex.orebfuscator.proximity.ProximitySystem;
import dev.imprex.orebfuscator.statistics.OrebfuscatorStatistics;
import dev.imprex.orebfuscator.statistics.StatisticsRegistry;
import dev.imprex.orebfuscator.util.Version;
import dev.imprex.orebfuscator.util.concurrent.OrebfuscatorExecutor;
import java.nio.file.Path;
import java.util.List;
import net.imprex.orebfuscator.api.OrebfuscatorService;
import net.imprex.orebfuscator.iterop.BukkitLoggerAccessor;
import net.imprex.orebfuscator.iterop.BukkitPlayerAccessorManager;
import net.imprex.orebfuscator.iterop.BukkitWorldAccessorManager;
import net.imprex.orebfuscator.obfuscation.ObfuscationSystem;
import net.imprex.orebfuscator.proximity.ProximityPacketListener;
import net.imprex.orebfuscator.util.MinecraftVersion;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public class Orebfuscator extends JavaPlugin implements Listener, OrebfuscatorCore {

  public static final ThreadGroup THREAD_GROUP = new ThreadGroup("orebfuscator");

  private StatisticsRegistry statisticsRegistry;
  private OrebfuscatorConfig config;
  private OrebfuscatorStatistics statistics;
  private OrebfuscatorExecutor executor;

  private BukkitWorldAccessorManager worldManager;
  private BukkitPlayerAccessorManager playerManager;

  private ChunkFactory chunkFactory;
  private ObfuscationProcessor obfuscationProcessor;
  private ObfuscationCache obfuscationCache;
  private ObfuscationPipeline obfuscationPipeline;
  private ObfuscationSystem obfuscationSystem;

  private ProximitySystem proximitySystem;
  private ProximityPacketListener proximityPacketListener;

  private Version orebfuscatorVersion;
  private UpdateSystem updateSystem;

  @Override
  public void onLoad() {
    OfcLogger.setLogger(new BukkitLoggerAccessor(getLogger()));
    this.orebfuscatorVersion = Version.parse(getDescription().getVersion());
  }

  @Override
  public void onEnable() {
    try {
      if (MinecraftVersion.isBelow("1.16")) {
        throw new RuntimeException("Orebfuscator only supports minecraft 1.16 and above");
      }

      Plugin protocolLib = getServer().getPluginManager().getPlugin("ProtocolLib");
      if (protocolLib == null || !protocolLib.isEnabled()) {
        throw new RuntimeException("ProtocolLib can't be found or is disabled! Orebfuscator can't be enabled.");
      }

      this.statisticsRegistry = new StatisticsRegistry();
      this.worldManager = new BukkitWorldAccessorManager(this);

      OrebfuscatorNms.initialize();
      this.config = new OrebfuscatorConfig(this);
      OrebfuscatorCompatibility.initialize(this, config);

      this.playerManager = new BukkitPlayerAccessorManager(this);
      new MetricsSystem(this);
      this.updateSystem = new UpdateSystem(this, "bukkit");

      this.statistics = new OrebfuscatorStatistics(this.config, this.statisticsRegistry);
      this.executor = new OrebfuscatorExecutor(this);

      this.chunkFactory = new ChunkFactory(this);
      this.obfuscationProcessor = new ObfuscationProcessor(this);
      this.obfuscationCache = new ObfuscationCache(this);
      this.obfuscationPipeline = new ObfuscationPipeline(this);
      this.obfuscationSystem = new ObfuscationSystem(this);

      this.proximitySystem = new ProximitySystem(this);
      if (this.config.proximityEnabled()) {
        this.proximitySystem.start();
        this.proximityPacketListener = new ProximityPacketListener(this);
      }

      // Load packet listener
      this.obfuscationSystem.registerChunkListener();
      this.config.store();

      // initialize service
      Bukkit.getServicesManager().register(
          OrebfuscatorService.class,
          new DefaultOrebfuscatorService(this),
          this, ServicePriority.Normal);

      // add commands
      getCommand("orebfuscator").setExecutor(new OrebfuscatorCommand(this));
    } catch (Exception e) {
      OfcLogger.error("An error occurred while enabling plugin", e);

      this.getServer().getPluginManager().registerEvent(PluginEnableEvent.class, this, EventPriority.NORMAL,
          this::onEnableFailed, this);
    }
  }

  @Override
  public void onDisable() {
    if (this.executor != null) {
      this.executor.shutdown();
    }

    if (this.obfuscationCache != null) {
      this.obfuscationCache.close();
    }

    if (this.obfuscationSystem != null) {
      this.obfuscationSystem.shutdown();
    }

    if (this.config != null && this.config.proximityEnabled() && this.proximityPacketListener != null) {
      this.proximityPacketListener.unregister();
    }

    OrebfuscatorCompatibility.close();

    this.config = null;
  }

  public void onEnableFailed(Listener listener, Event event) {
    PluginEnableEvent enableEvent = (PluginEnableEvent) event;

    if (enableEvent.getPlugin() == this) {
      HandlerList.unregisterAll(listener);
      Bukkit.getPluginManager().disablePlugin(this);
    }
  }

  public UpdateSystem updateSystem() {
    return updateSystem;
  }

  public ObfuscationSystem getObfuscationSystem() {
    return obfuscationSystem;
  }

  public BukkitWorldAccessorManager worldManager() {
    return worldManager;
  }

  public BukkitPlayerAccessorManager playerManager() {
    return playerManager;
  }

  @Override
  public OrebfuscatorExecutor executor() {
    return executor;
  }

  @Override
  public StatisticsRegistry statisticsRegistry() {
    return statisticsRegistry;
  }

  @Override
  public OrebfuscatorStatistics statistics() {
    return statistics;
  }

  @Override
  public OrebfuscatorConfig config() {
    return config;
  }

  @Override
  public ChunkFactory chunkFactory() {
    return chunkFactory;
  }

  @Override
  public ObfuscationCache cache() {
    return obfuscationCache;
  }

  @Override
  public ObfuscationPipeline obfuscationPipeline() {
    return obfuscationPipeline;
  }

  @Override
  public ObfuscationProcessor obfuscationProcessor() {
    return obfuscationProcessor;
  }

  @Override
  public boolean isGameThread() {
    return OrebfuscatorCompatibility.isGameThread();
  }

  @Override
  public Path configDirectory() {
    return getDataFolder().toPath();
  }

  @Override
  public Path worldDirectory() {
    return Bukkit.getWorldContainer().toPath();
  }

  @Override
  public String name() {
    return getDescription().getName();
  }

  @Override
  public Version orebfuscatorVersion() {
    return this.orebfuscatorVersion;
  }

  @Override
  public Version minecraftVersion() {
    return MinecraftVersion.current();
  }

  @Override
  public RegistryAccessor registry() {
    return OrebfuscatorNms.registry();
  }

  @Override
  public AbstractRegionFileCache<?> createRegionFileCache() {
    return OrebfuscatorNms.createRegionFileCache(config);
  }

  @Override
  public List<WorldAccessor> worlds() {
    return this.worldManager.all();
  }

  @Override
  public List<PlayerAccessor> players() {
    return this.playerManager.all();
  }

  @Override
  public String toString() {
    var meta = getDescription();
    return String.format("%s %s", meta.getName(), meta.getVersion());
  }
}