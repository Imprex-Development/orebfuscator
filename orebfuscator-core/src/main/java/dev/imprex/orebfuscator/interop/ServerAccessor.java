package dev.imprex.orebfuscator.interop;

import java.nio.file.Path;
import java.util.List;
import dev.imprex.orebfuscator.cache.AbstractRegionFileCache;
import dev.imprex.orebfuscator.util.Version;

public interface ServerAccessor {

  boolean isGameThread();

  Path getConfigDirectory();

  Path getWorldDirectory();

  Version getOrebfuscatorVersion();

  Version getMinecraftVersion();

  RegistryAccessor getRegistry();

  AbstractRegionFileCache<?> createRegionFileCache();

  List<WorldAccessor> getWorlds();

}
