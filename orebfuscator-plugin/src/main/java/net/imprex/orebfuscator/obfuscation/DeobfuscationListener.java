package net.imprex.orebfuscator.obfuscation;

import java.util.BitSet;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
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

	public static void createAndRegister(Orebfuscator orebfuscator, DeobfuscationWorker deobfuscationWorker) {
		Listener listener = new DeobfuscationListener(orebfuscator, deobfuscationWorker);
		Bukkit.getPluginManager().registerEvents(listener, orebfuscator);
	}

	private final UpdateSystem updateSystem;
	private final OrebfuscatorConfig config;
	private final DeobfuscationWorker deobfuscationWorker;

	private final BitSet occludingFallable = new BitSet();

	private DeobfuscationListener(Orebfuscator orebfuscator, DeobfuscationWorker deobfuscationWorker) {
		this.updateSystem = orebfuscator.getUpdateSystem();
		this.config = orebfuscator.getOrebfuscatorConfig();
		this.deobfuscationWorker = deobfuscationWorker;

		for (Material material : Material.values()) {
			for (int blockId : NmsInstance.getBlockIds(material)) {
				if (NmsInstance.isFallable(blockId) && NmsInstance.isOccluding(blockId)) {
					this.occludingFallable.set(material.ordinal());
				}
			}
		}
	}

	@EventHandler
	public void onBlockDamage(BlockDamageEvent event) {
		if (this.config.general().updateOnBlockDamage()) {
			this.deobfuscationWorker.deobfuscate(event.getBlock());
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		this.deobfuscationWorker.deobfuscate(event.getBlock());
	}

	@EventHandler
	public void onBlockBurn(BlockBurnEvent event) {
		this.deobfuscationWorker.deobfuscate(event.getBlock());
	}

	@EventHandler
	public void onBlockExplode(BlockExplodeEvent event) {
		this.deobfuscationWorker.deobfuscate(event.blockList(), true);
	}

	@EventHandler
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		this.deobfuscationWorker.deobfuscate(event.getBlocks(), true);
	}

	@EventHandler
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {
		this.deobfuscationWorker.deobfuscate(event.getBlocks(), true);
	}

	@EventHandler
	public void onBlockPhysics(BlockPhysicsEvent event) {
		if (this.occludingFallable.get(event.getBlock().getType().ordinal())) {
			this.deobfuscationWorker.deobfuscate(event.getBlock());
		}
	}

	@EventHandler
	public void onEntityExplode(EntityExplodeEvent event) {
		this.deobfuscationWorker.deobfuscate(event.blockList(), true);
	}

	@EventHandler
	public void onEntityInteract(EntityInteractEvent event) {
		this.deobfuscationWorker.deobfuscate(event.getBlock());
	}

	@EventHandler
	public void onEntityChangeBlock(EntityChangeBlockEvent event) {
		this.deobfuscationWorker.deobfuscate(event.getBlock());
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.useInteractedBlock() != Result.DENY
				&& event.getItem() != null && event.getItem().getType() != null
				&& NmsInstance.isHoe(event.getItem().getType())) {
			this.deobfuscationWorker.deobfuscate(event.getClickedBlock());
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if (this.config.general().bypassNotification() && PermissionUtil.canDeobfuscate(player)) {
			player.sendMessage(
					"[§bOrebfuscator§f]§7 You bypass Orebfuscator because you have the 'orebfuscator.bypass' permission.");
		}

		if (PermissionUtil.canCheckForUpdates(player)) {
			this.updateSystem.checkForUpdates(player);
		}
	}
}
