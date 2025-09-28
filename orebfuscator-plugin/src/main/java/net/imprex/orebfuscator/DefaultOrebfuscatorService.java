package net.imprex.orebfuscator;

import java.util.Collection;
import java.util.Iterator;

import org.bukkit.World;
import org.bukkit.block.Block;

import net.imprex.orebfuscator.api.OrebfuscatorService;
import net.imprex.orebfuscator.obfuscation.ObfuscationSystem;

public final class DefaultOrebfuscatorService implements OrebfuscatorService {

  private final ObfuscationSystem obfuscationSystem;

  public DefaultOrebfuscatorService(Orebfuscator orebfuscator) {
    this.obfuscationSystem = orebfuscator.getObfuscationSystem();
  }

  @Override
  public final void deobfuscate(Collection<? extends Block> blocks) {
    if (!OrebfuscatorCompatibility.isGameThread()) {
      throw new IllegalStateException("Asynchronous deobfuscation! " + Thread.currentThread());
    } else if (blocks == null || blocks.isEmpty()) {
      throw new IllegalArgumentException("block list is null or empty");
    }

    Iterator<? extends Block> blockIterator = blocks.iterator();
    World world = blockIterator.next().getWorld();
    while (blockIterator.hasNext()) {
      if (blockIterator.next().getWorld() != world) {
        throw new IllegalArgumentException("block list is located in more than one world");
      }
    }

    this.obfuscationSystem.deobfuscate(blocks);
  }
}
