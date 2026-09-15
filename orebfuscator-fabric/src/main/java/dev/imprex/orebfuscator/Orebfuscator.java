package dev.imprex.orebfuscator;

import dev.imprex.orebfuscator.cache.AbstractRegionFileCache;
import dev.imprex.orebfuscator.cache.ObfuscationCache;
import dev.imprex.orebfuscator.chunk.ChunkFactory;
import dev.imprex.orebfuscator.config.OrebfuscatorConfig;
import dev.imprex.orebfuscator.injector.OrebfuscatorInjector;
import dev.imprex.orebfuscator.interop.OrebfuscatorCore;
import dev.imprex.orebfuscator.interop.PlayerAccessor;
import dev.imprex.orebfuscator.interop.RegistryAccessor;
import dev.imprex.orebfuscator.interop.WorldAccessor;
import dev.imprex.orebfuscator.iterop.FabricPlayerAccessor;
import dev.imprex.orebfuscator.iterop.FabricRegionFileCache;
import dev.imprex.orebfuscator.iterop.FabricRegistryAccessor;
import dev.imprex.orebfuscator.iterop.FabricWorldAccessor;
import dev.imprex.orebfuscator.obfuscation.ObfuscationPipeline;
import dev.imprex.orebfuscator.obfuscation.ObfuscationProcessor;
import dev.imprex.orebfuscator.obfuscation.ObfuscationSystem;
import dev.imprex.orebfuscator.proximity.ProximitySystem;
import dev.imprex.orebfuscator.statistics.InjectorStatistics;
import dev.imprex.orebfuscator.statistics.OrebfuscatorStatistics;
import dev.imprex.orebfuscator.statistics.StatisticsRegistry;
import dev.imprex.orebfuscator.util.Version;
import dev.imprex.orebfuscator.util.WrappedClientboundLevelChunkPacketData;
import dev.imprex.orebfuscator.util.concurrent.OrebfuscatorExecutor;

import java.nio.file.Path;
import java.util.List;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class Orebfuscator implements OrebfuscatorCore, AutoCloseable {

  public static final ThreadGroup THREAD_GROUP = new ThreadGroup("orebfuscator");

  public static InjectorStatistics injectorStatistics = new InjectorStatistics();

  private final MinecraftServer server;
  private final ModContainer modContainer;
  private final Version gameVersion;

  private final String name;
  private final Version version;

  private final OrebfuscatorExecutor executor;
  private final SystemMonitor systemMonitor;
  private final FabricRegistryAccessor registry;
  private final StatisticsRegistry statisticsRegistry;
  private final OrebfuscatorStatistics statistics;
  private final OrebfuscatorConfig config;
  private final ObfuscationCache obfuscationCache;
  private final ObfuscationSystem obfuscationSystem;
  private final ProximitySystem proximitySystem;
  private final ChunkFactory chunkFactory;
  private final ObfuscationPipeline obfuscationPipeline;
  private final ObfuscationProcessor obfuscationProcessor;

  public Orebfuscator(MinecraftServer server, ModContainer modContainer) {
    this.server = server;
    this.modContainer = modContainer;
    this.gameVersion = Version.parse(FabricLoader.getInstance().getRawGameVersion());

    this.name = modContainer.getMetadata().getName();
    this.version = Version.parse(modContainer.getMetadata().getVersion().toString());

    WrappedClientboundLevelChunkPacketData.init();
    FabricWorldAccessor.registerListener(server, this);

    this.registry = new FabricRegistryAccessor();
    this.statisticsRegistry = new StatisticsRegistry();
    this.config = new OrebfuscatorConfig(this);
    this.statistics = new OrebfuscatorStatistics(this.config, this.statisticsRegistry);
    this.executor = new OrebfuscatorExecutor(this);
    this.systemMonitor = new SystemMonitor(this);
    this.chunkFactory = new ChunkFactory(this);
    this.obfuscationProcessor = new ObfuscationProcessor(this);
    this.obfuscationCache = new ObfuscationCache(this);
    this.obfuscationPipeline = new ObfuscationPipeline(this);
    this.obfuscationSystem = new ObfuscationSystem(this);

    Orebfuscator.injectorStatistics = this.statistics.injector;

    FabricPlayerAccessor.registerListener(this);

    this.proximitySystem = new ProximitySystem(this);
    if (this.config.proximityEnabled()) {
      this.proximitySystem.start();
    }

    new OrebfuscatorInjector(this);
    this.config.store();
  }

  @Override
  public void close() {
    this.obfuscationCache.close();
  }

  public MinecraftServer server() {
    return server;
  }

  public ObfuscationSystem getObfuscationSystem() {
    return obfuscationSystem;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public OrebfuscatorExecutor executor() {
    return executor;
  }

  @Override
  public SystemMonitor systemMonitor() {
    return systemMonitor;
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
    return server.isSameThread();
  }

  @Override
  public Path configDirectory() {
    return FabricLoader.getInstance().getConfigDir().resolve("orebfuscator");
  }

  @Override
  public Path worldDirectory() {
    return FabricLoader.getInstance().getGameDir();
  }

  @Override
  public Version orebfuscatorVersion() {
    return this.version;
  }

  @Override
  public Version minecraftVersion() {
    return this.gameVersion;
  }

  @Override
  public RegistryAccessor registry() {
    return registry;
  }

  @Override
  public AbstractRegionFileCache<?> createRegionFileCache() {
    return new FabricRegionFileCache(config, server);
  }

  @Override
  public List<WorldAccessor> worlds() {
    return FabricWorldAccessor.getLevels().stream().map(WorldAccessor.class::cast).toList();
  }

  @Override
  public List<PlayerAccessor> players() {
    return FabricPlayerAccessor.getAll().stream().map(PlayerAccessor.class::cast).toList();
  }

  @Override
  public String toString() {
    var meta = modContainer.getMetadata();
    return String.format("%s %s", meta.getName(), meta.getVersion());
  }
}
