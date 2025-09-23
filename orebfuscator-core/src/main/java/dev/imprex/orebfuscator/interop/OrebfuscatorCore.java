package dev.imprex.orebfuscator.interop;

import dev.imprex.orebfuscator.cache.ObfuscationCache;
import dev.imprex.orebfuscator.chunk.ChunkFactory;
import dev.imprex.orebfuscator.config.OrebfuscatorConfig;
import dev.imprex.orebfuscator.obfuscation.ObfuscationPipeline;
import dev.imprex.orebfuscator.obfuscation.ObfuscationProcessor;
import dev.imprex.orebfuscator.statistics.OrebfuscatorStatistics;

public interface OrebfuscatorCore extends ServerAccessor {

  ThreadGroup THREAD_GROUP = new ThreadGroup("orebfuscator");

  OrebfuscatorStatistics statistics();

  OrebfuscatorConfig config();

  ChunkFactory chunkFactory();

  ObfuscationCache cache();

  ObfuscationPipeline obfuscationPipeline();

  ObfuscationProcessor obfuscationProcessor();

}
