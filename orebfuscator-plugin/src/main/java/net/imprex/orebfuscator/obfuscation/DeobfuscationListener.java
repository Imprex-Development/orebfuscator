package net.imprex.orebfuscator.obfuscation;

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
import net.imprex.orebfuscator.UpdateSystem;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.util.PermissionUtil;

public class DeobfuscationListener implements Listener {

	private final UpdateSystem updateSystem;
	private final OrebfuscatorConfig config;
	private final Deobfuscator deobfuscator;

	public DeobfuscationListener(Orebfuscator orebfuscator, Deobfuscator deobfuscator) {
		this.updateSystem = orebfuscator.getUpdateSystem();
		this.config = orebfuscator.getOrebfuscatorConfig();
		this.deobfuscator = deobfuscator;
	}

	@EventHandler
	public void onBlockDamage(BlockDamageEvent event) {
		if (this.config.general().updateOnBlockDamage()) {
			this.deobfuscator.deobfuscate(event.getBlock());
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		this.deobfuscator.deobfuscate(event.getBlock());
	}

	@EventHandler
	public void onBlockBurn(BlockBurnEvent event) {
		this.deobfuscator.deobfuscate(event.getBlock());
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent event) {
		this.deobfuscator.deobfuscate(event.blockList(), true);
	}

	@EventHandler
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		this.deobfuscator.deobfuscate(event.getBlocks(), true);
	}

	@EventHandler
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {
		this.deobfuscator.deobfuscate(event.getBlocks(), true);
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		this.deobfuscator.deobfuscate(event.blockList(), true);
	}

	@EventHandler
	public void onEntityInteract(EntityInteractEvent event) {
		this.deobfuscator.deobfuscate(event.getBlock());
	}

	@EventHandler
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
		this.deobfuscator.deobfuscate(event.getBlock());
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.useInteractedBlock() != Result.DENY
				&& event.getItem() != null && event.getItem().getType() != null
				&& NmsInstance.isHoe(event.getItem().getType())) {
			this.deobfuscator.deobfuscate(event.getClickedBlock());
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if (this.config.general().bypassNotification() && PermissionUtil.canDeobfuscate(player)) {
			player.sendMessage("[§bOrebfuscator§f]§7 Orebfuscator bypassed.");
		}

		if (PermissionUtil.canCheckForUpdates(player)) {
			this.updateSystem.checkForUpdates(player);
		}
	}
}
