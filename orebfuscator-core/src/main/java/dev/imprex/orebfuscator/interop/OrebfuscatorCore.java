package dev.imprex.orebfuscator.interop;

import dev.imprex.orebfuscator.SystemMonitor;
import dev.imprex.orebfuscator.statistics.StatisticsRegistry;
import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.cache.ObfuscationCache;
import dev.imprex.orebfuscator.chunk.ChunkFactory;
import dev.imprex.orebfuscator.config.OrebfuscatorConfig;
import dev.imprex.orebfuscator.obfuscation.ObfuscationPipeline;
import dev.imprex.orebfuscator.obfuscation.ObfuscationProcessor;
import dev.imprex.orebfuscator.statistics.OrebfuscatorStatistics;
import dev.imprex.orebfuscator.util.concurrent.OrebfuscatorExecutor;

@NullMarked
public interface OrebfuscatorCore extends ServerAccessor {

  ThreadGroup THREAD_GROUP = new ThreadGroup("orebfuscator");

  String name();

  OrebfuscatorExecutor executor();

  SystemMonitor systemMonitor();

  StatisticsRegistry statisticsRegistry();

  OrebfuscatorStatistics statistics();

  OrebfuscatorConfig config();

  ChunkFactory chunkFactory();

  ObfuscationCache cache();

  ObfuscationPipeline obfuscationPipeline();

  ObfuscationProcessor obfuscationProcessor();

}
