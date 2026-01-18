package dev.imprex.orebfuscator.interop;

import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.cache.AbstractRegionFileCache;
import dev.imprex.orebfuscator.util.Version;

@NullMarked
public interface ServerAccessor {

  boolean isGameThread();

  Path configDirectory();

  Path worldDirectory();

  Version orebfuscatorVersion();

  Version minecraftVersion();

  RegistryAccessor registry();

  AbstractRegionFileCache<?> createRegionFileCache();

  List<WorldAccessor> worlds();

  List<PlayerAccessor> players();
}
