package net.imprex.orebfuscator.compatibility;

import com.sk89q.worldedit.EditSession.Stage;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.event.extent.EditSessionEvent;
import com.sk89q.worldedit.extent.AbstractDelegateExtent;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.RunContext;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.util.eventbus.Subscribe;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import dev.imprex.orebfuscator.util.BlockPos;
import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;
import net.imprex.orebfuscator.obfuscation.ObfuscationSystem;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** WorldEdit writes and lifecycle callbacks must run on the Bukkit main thread. */
@NullMarked
public class WorldEditListener {

  private static final int BATCH_SIZE = 16 * 1024;
  private static final long[] EMPTY_BLOCKS = new long[0];

  private final Plugin plugin;
  private final ObfuscationSystem obfuscationSystem;
  private final Queue<Batch> pendingBatches = new ArrayDeque<>();

  private @Nullable BukkitTask task;
  private boolean stopped;

  public WorldEditListener(Plugin plugin, ObfuscationSystem obfuscationSystem) {
    this.plugin = plugin;
    this.obfuscationSystem = obfuscationSystem;
    WorldEdit.getInstance().getEventBus().register(this);
  }

  @Subscribe
  public void onEditSession(EditSessionEvent event) {
    if (this.stopped
        || event.getStage() != Stage.BEFORE_CHANGE
        || !(event.getWorld() instanceof BukkitWorld world)) {
      return;
    }

    World bukkitWorld = world.getWorld();
    event.setExtent(new AbstractDelegateExtent(event.getExtent()) {
      private long[] blocks = EMPTY_BLOCKS;
      private int size;

      @Override
      public <T extends BlockStateHolder<T>> boolean setBlock(BlockVector3 position, T block)
          throws WorldEditException {
        if (!super.setBlock(position, block)) {
          return false;
        }

        if (this.size == this.blocks.length) {
          var capacity = Math.min(BATCH_SIZE, Math.max(256, this.blocks.length * 2));
          this.blocks = Arrays.copyOf(this.blocks, capacity);
        }

        this.blocks[this.size++] = BlockPos.toLong(position.getX(), position.getY(), position.getZ());

        if (this.size == BATCH_SIZE) {
          flush();
        }
        return true;
      }

      @Override
      protected Operation commitBefore() {
        // Reordered writes reach this extent only when the commit operations run.
        return new Operation() {
          @Override
          public @Nullable Operation resume(RunContext run) {
            flush();
            return null;
          }

          @Override
          public void cancel() {
            flush();
          }
        };
      }

      private void flush() {
        if (this.size > 0) {
          enqueue(bukkitWorld, this.blocks, this.size);
          this.blocks = EMPTY_BLOCKS;
          this.size = 0;
        }
      }
    });
  }

  private void enqueue(World world, long[] blocks, int size) {
    if (this.stopped || !this.plugin.isEnabled()) {
      return;
    }

    if (size != blocks.length) {
      blocks = Arrays.copyOf(blocks, size);
    }

    this.pendingBatches.add(new Batch(world, blocks));

    if (this.task == null) {
      this.task = Bukkit.getScheduler().runTaskTimer(this.plugin, this::processBatch, 1L, 1L);
    }
  }

  private void processBatch() {
    try {
      Batch batch = this.pendingBatches.poll();
      if (batch != null && Bukkit.getWorld(batch.world().getUID()) == batch.world()) {

        final var blocks = batch.blocks();
        this.obfuscationSystem.deobfuscate(batch.world(), new AbstractList<BlockPos>() {
          @Override
          public BlockPos get(int index) {
            return BlockPos.fromLong(blocks[index]);
          }

          @Override
          public int size() {
            return blocks.length;
          }
        });
      }
    } finally {
      if (this.pendingBatches.isEmpty() && this.task != null) {
        this.task.cancel();
        this.task = null;
      }
    }
  }

  public void unregister() {
    this.stopped = true;
    WorldEdit.getInstance().getEventBus().unregister(this);

    if (this.task != null) {
      this.task.cancel();
      this.task = null;
    }

    this.pendingBatches.clear();
  }

  private record Batch(World world, long[] blocks) {}
}
