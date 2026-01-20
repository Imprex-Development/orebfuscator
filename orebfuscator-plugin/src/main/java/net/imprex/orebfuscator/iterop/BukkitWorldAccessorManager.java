package net.imprex.orebfuscator.iterop;

import dev.imprex.orebfuscator.interop.WorldAccessor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.imprex.orebfuscator.Orebfuscator;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class BukkitWorldAccessorManager implements Listener {

  private final Map<World, BukkitWorldAccessor> worlds = new HashMap<>();

  private final Orebfuscator orebfuscator;

  public BukkitWorldAccessorManager(Orebfuscator orebfuscator) {
    this.orebfuscator = orebfuscator;
    Bukkit.getPluginManager().registerEvents(this, orebfuscator);

    for (World world : Bukkit.getWorlds()) {
      this.worlds.put(world, new BukkitWorldAccessor(world, orebfuscator));
    }
  }

  public List<WorldAccessor> all() {
    return this.worlds.values().stream()
        .map(WorldAccessor.class::cast)
        .toList();
  }

  public BukkitWorldAccessor get(World world) {
    var bukkitWorld = this.worlds.get(world);
    if (bukkitWorld == null) {
      throw new IllegalStateException("Can't find accessor for world " + world.getName());
    }
    return bukkitWorld;
  }

  @EventHandler
  public void onWorldLoad(WorldLoadEvent event) {
    this.worlds.put(event.getWorld(), new BukkitWorldAccessor(event.getWorld(), this.orebfuscator));
  }

  @EventHandler
  public void onWorldUnload(WorldUnloadEvent event) {
    this.worlds.remove(event.getWorld());
  }

  @EventHandler
  public void onDisable(PluginDisableEvent event) {
    if (event.getPlugin() == this.orebfuscator) {
      this.worlds.clear();
    }
  }
}
