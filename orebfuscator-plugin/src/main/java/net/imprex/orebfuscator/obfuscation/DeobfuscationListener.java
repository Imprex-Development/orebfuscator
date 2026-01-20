package net.imprex.orebfuscator.obfuscation;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jspecify.annotations.NullMarked;
import dev.imprex.orebfuscator.PermissionRequirements;
import dev.imprex.orebfuscator.UpdateSystem;
import dev.imprex.orebfuscator.config.OrebfuscatorConfig;
import dev.imprex.orebfuscator.util.ConsoleUtil;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorCompatibility;
import net.imprex.orebfuscator.util.PermissionUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;

@NullMarked
public class DeobfuscationListener implements Listener {

  public static void createAndRegister(Orebfuscator orebfuscator, ObfuscationSystem obfuscationSystem) {
    Listener listener = new DeobfuscationListener(orebfuscator, obfuscationSystem);
    Bukkit.getPluginManager().registerEvents(listener, orebfuscator);
  }

  private final UpdateSystem updateSystem;
  private final OrebfuscatorConfig config;
  private final ObfuscationSystem obfuscationSystem;

  private DeobfuscationListener(Orebfuscator orebfuscator, ObfuscationSystem obfuscationSystem) {
    this.updateSystem = orebfuscator.updateSystem();
    this.config = orebfuscator.config();
    this.obfuscationSystem = obfuscationSystem;
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onBlockDamage(BlockDamageEvent event) {
    if (this.config.general().updateOnBlockDamage()) {
      this.obfuscationSystem.deobfuscate(event.getBlock());
    }
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onBlockBreak(BlockBreakEvent event) {
    this.obfuscationSystem.deobfuscate(event.getBlock());
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onBlockBurn(BlockBurnEvent event) {
    this.obfuscationSystem.deobfuscate(event.getBlock());
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onBlockExplode(BlockExplodeEvent event) {
    this.obfuscationSystem.deobfuscate(event.blockList());
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onBlockPistonExtend(BlockPistonExtendEvent event) {
    this.obfuscationSystem.deobfuscate(event.getBlocks());
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onBlockPistonRetract(BlockPistonRetractEvent event) {
    this.obfuscationSystem.deobfuscate(event.getBlocks());
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEntityExplode(EntityExplodeEvent event) {
    this.obfuscationSystem.deobfuscate(event.blockList());
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEntityChangeBlock(EntityChangeBlockEvent event) {
    this.obfuscationSystem.deobfuscate(event.getBlock());
  }

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onPlayerInteract(PlayerInteractEvent event) {
    if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.useInteractedBlock() != Result.DENY
        && event.getItem() != null && event.getItem().getType() != null) {
      Material material = event.getItem().getType();
      if (material.name().endsWith("_HOE")) {
        this.obfuscationSystem.deobfuscate(event.getClickedBlock());
      }
    }
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();

    if (this.config.general().bypassNotification() && PermissionUtil.hasPermission(player, PermissionRequirements.BYPASS)) {
      player.sendMessage(
          "[§bOrebfuscator§f]§7 You bypass Orebfuscator because you have the 'orebfuscator.bypass' permission.");
    }

    if (PermissionUtil.hasPermission(player, PermissionRequirements.ADMIN)) {
      String configReport = this.config.report();
      if (configReport != null) {
        player.sendMessage("[§bOrebfuscator§f]§c " + ConsoleUtil.replaceAnsiColorWithChatColor(configReport));
      }

      this.updateSystem.ifNewerDownloadAvailable(downloadUri -> {
        BaseComponent[] components = new ComponentBuilder("[§bOrebfuscator§f]§7 A new release is available ")
            .append("§f§l[CLICK HERE]")
            .event(new ClickEvent(ClickEvent.Action.OPEN_URL, downloadUri))
            .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(new ComponentBuilder("§7Click here to see the latest release").create()))).create();
        OrebfuscatorCompatibility.runForPlayer(player, () -> {
          player.spigot().sendMessage(components);
        });
      });
    }
  }
}
