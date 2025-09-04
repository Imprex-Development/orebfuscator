package dev.imprex.orebfuscator.interop;

import java.nio.file.Path;
import java.util.List;
import dev.imprex.orebfuscator.util.Version;

public interface ServerAccessor {

  Path getConfigDirectory();

  Path getWorldDirectory();

  String getOrebfuscatorVersion();

  Version getMinecraftVersion();

  RegistryAccessor getRegistry();

  List<WorldAccessor> getWorlds();

}
