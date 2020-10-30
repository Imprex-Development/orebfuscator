package net.imprex.orebfuscator.obfuscation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.cache.ChunkCache;
import net.imprex.orebfuscator.config.BlockMask;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.WorldConfig;
import net.imprex.orebfuscator.nms.BlockStateHolder;
import net.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.util.PermissionUtil;

public class DeobfuscationListener implements Listener {

	private final OrebfuscatorConfig config;
	private final ChunkCache chunkCache;

	public DeobfuscationListener(Orebfuscator orebfuscator) {
		this.config = orebfuscator.getOrebfuscatorConfig();
		this.chunkCache = orebfuscator.getChunkCache();
	}

	private void onUpdate(Block block) {
		if (block == null || !block.getType().isOccluding()) {
			return;
		}

		onUpdate(Arrays.asList(block));
	}

	private void onUpdate(List<Block> blocks) {
		if (blocks.isEmpty()) {
			return;
		}

		World world = blocks.get(0).getWorld();
		WorldConfig worldConfig = this.config.world(world);
		if (worldConfig == null || !worldConfig.enabled()) {
			return;
		}

		BlockMask blockMask = this.config.blockMask(world);

		Set<BlockStateHolder> updateBlocks = new HashSet<>();
		Set<ChunkPosition> invalidChunks = new HashSet<>();
		int updateRadius = this.config.general().updateRadius();

		for (Block block : blocks) {
			if (block.getType().isOccluding()) {
				int x = block.getX();
				int y = block.getY();
				int z = block.getZ();

				BlockStateHolder blockState = NmsInstance.getBlockState(world, x, y, z);
				if (blockState != null) {
					getAdjacentBlocks(updateBlocks, world, blockMask, blockState, updateRadius);
					invalidChunks.add(new ChunkPosition(world, x >> 4, z >> 4));
				}
			}
		}

		for (BlockStateHolder blockState : updateBlocks) {
			blockState.notifyBlockChange();
		}

		if (!invalidChunks.isEmpty() && config.cache().enabled()) {
			for (ChunkPosition chunk : invalidChunks) {
				chunkCache.invalidate(chunk);
			}
		}
	}

	private void getAdjacentBlocks(Set<BlockStateHolder> updateBlocks, World world, BlockMask blockMask,
			BlockStateHolder blockState, int depth) {
		if (blockState == null) {
			return;
		}

		int blockId = blockState.getBlockId();
		if ((blockMask.mask(blockId) & BlockMask.FLAG_OBFUSCATE) != 0) {
			updateBlocks.add(blockState);
		}

		if (depth-- > 0) {
			int x = blockState.getX();
			int y = blockState.getY();
			int z = blockState.getZ();

			getAdjacentBlocks(updateBlocks, world, blockMask, NmsInstance.getBlockState(world, x + 1, y, z), depth);
			getAdjacentBlocks(updateBlocks, world, blockMask, NmsInstance.getBlockState(world, x - 1, y, z), depth);
			getAdjacentBlocks(updateBlocks, world, blockMask, NmsInstance.getBlockState(world, x, y + 1, z), depth);
			getAdjacentBlocks(updateBlocks, world, blockMask, NmsInstance.getBlockState(world, x, y - 1, z), depth);
			getAdjacentBlocks(updateBlocks, world, blockMask, NmsInstance.getBlockState(world, x, y, z + 1), depth);
			getAdjacentBlocks(updateBlocks, world, blockMask, NmsInstance.getBlockState(world, x, y, z - 1), depth);
		}
	}

	@EventHandler
	public void onBlockDamage(BlockDamageEvent event) {
		if (this.config.general().updateOnBlockDamage()) {
			this.onUpdate(event.getBlock());
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		this.onUpdate(event.getBlock());
	}

	@EventHandler
	public void onBlockBurn(BlockBurnEvent event) {
		this.onUpdate(event.getBlock());
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent event) {
		this.onUpdate(event.blockList());
	}

	@EventHandler
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		this.onUpdate(event.getBlocks());
	}

	@EventHandler
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {
		this.onUpdate(event.getBlocks());
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		this.onUpdate(event.blockList());
	}

	@EventHandler
	public void onEntityInteract(EntityInteractEvent event) {
		this.onUpdate(event.getBlock());
	}

	@EventHandler
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
		this.onUpdate(event.getBlock());
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.useInteractedBlock() != Result.DENY
				&& event.getItem() != null && event.getItem().getType() != null
				&& NmsInstance.isHoe(event.getItem().getType())) {
			this.onUpdate(event.getClickedBlock());
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (this.config.general().bypassNotification() && PermissionUtil.canDeobfuscate(player)) {
			player.sendMessage("[OFC] Orebfuscator bypassed.");
		}
	}
}
