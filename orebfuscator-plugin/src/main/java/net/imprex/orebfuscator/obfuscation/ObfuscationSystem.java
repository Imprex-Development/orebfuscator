package net.imprex.orebfuscator.obfuscation;

import java.util.Collection;
import org.bukkit.block.Block;
import dev.imprex.orebfuscator.obfuscation.DeobfuscationWorker;
import dev.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.iterop.BukkitWorldAccessor;

// TODO: Nullability
public class ObfuscationSystem {

  private final Orebfuscator orebfuscator;
  private ObfuscationListener listener;

  private final DeobfuscationWorker deobfuscationWorker;

  public ObfuscationSystem(Orebfuscator orebfuscator) {
    this.orebfuscator = orebfuscator;

    this.deobfuscationWorker = new DeobfuscationWorker(orebfuscator);
    DeobfuscationListener.createAndRegister(orebfuscator, this);
  }

  public void registerChunkListener() {
    this.listener = new ObfuscationListener(orebfuscator);
  }
  
  public void deobfuscate(Block block) {
    var world = BukkitWorldAccessor.get(block.getWorld());
    var blockPos = new BlockPos(block.getX(), block.getY(), block.getZ());
    this.deobfuscationWorker.deobfuscate(world, blockPos);
  }
  
  public void deobfuscate(Collection<? extends Block> blocks) {
    if (blocks.isEmpty()) {
      return;
    }
    
    var world = BukkitWorldAccessor.get(blocks.stream().findFirst().get().getWorld());
    var blockPos = blocks.stream()
        .map(block -> new BlockPos(block.getX(), block.getY(), block.getZ()))
        .toList();

    this.deobfuscationWorker.deobfuscate(world, blockPos);
  }

  public void shutdown() {
    this.listener.unregister();
  }
}
