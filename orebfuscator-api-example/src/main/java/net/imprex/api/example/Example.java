package net.imprex.api.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import net.imprex.orebfuscator.api.OrebfuscatorService;

public class Example extends JavaPlugin implements Listener {

  private static final Logger LOGGER = Logger.getLogger("bukkit.orebfuscator-api-example");

  private OrebfuscatorService orebfuscatorService;

  @Override
  public void onEnable() {
    ServicesManager serviceManager = getServer().getServicesManager();
    if (!serviceManager.isProvidedFor(OrebfuscatorService.class)) {
      LOGGER.severe("OrebfuscatorService not found! Plugin cannot be enabled.");
      return;
    }

    this.orebfuscatorService = serviceManager.load(OrebfuscatorService.class);
    Bukkit.getPluginManager().registerEvents(this, this);
  }

  @EventHandler
  public void onPlayerInteract(PlayerInteractEvent event) {
    ItemStack item = event.getItem();
    if (event.getAction() == Action.RIGHT_CLICK_BLOCK && item != null && event.getHand() == EquipmentSlot.HAND) {
      Block block = event.getClickedBlock();

      if (item.getType() == Material.DIAMOND_PICKAXE) {
        List<Block> blocks = this.getBlocks(block, 2);
        blocks.forEach(b -> b.setType(Material.AIR));
        this.orebfuscatorService.deobfuscate(blocks);
        event.setCancelled(true);
      } else if (item.getType() == Material.WOODEN_PICKAXE) {
        block.setType(Material.AIR);
        this.orebfuscatorService.deobfuscate(Arrays.asList(block));
        event.setCancelled(true);
      }
    }
  }

  private List<Block> getBlocks(Block origin, int size) {
    List<Block> blocks = new ArrayList<Block>();

    blocks.add(origin);

    for (int x = -size; x <= size; x++) {
      for (int y = -size; y <= size; y++) {
        for (int z = -size; z <= size; z++) {
          Block relative = origin.getRelative(x, y, z);
          if (!relative.getType().isAir()) {
            blocks.add(relative);
          }
        }
      }
    }

    return blocks;
  }
}
