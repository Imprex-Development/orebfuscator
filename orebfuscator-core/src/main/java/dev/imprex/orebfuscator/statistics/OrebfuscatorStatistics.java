package dev.imprex.orebfuscator.statistics;

import dev.imprex.orebfuscator.config.api.Config;

public class OrebfuscatorStatistics {

  public final CacheStatistics cache = new CacheStatistics();
  public final InjectorStatistics injector = new InjectorStatistics();
  public final ObfuscationStatistics obfuscation = new ObfuscationStatistics();

  public OrebfuscatorStatistics(Config config, StatisticsRegistry registry) {
    if (config.cache().enabled()) {
      registry.register("cache", this.cache);
    }

    registry.register("injector", this.injector);
    registry.register("obfuscation", this.obfuscation);
  }
}
