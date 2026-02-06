package net.imprex.orebfuscator;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketListener;
import dev.imprex.orebfuscator.OrebfuscatorDumpFile;
import dev.imprex.orebfuscator.PermissionRequirements;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import net.imprex.orebfuscator.util.MinecraftVersion;
import net.imprex.orebfuscator.util.PermissionUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.jspecify.annotations.NonNull;

public class OrebfuscatorCommand implements CommandExecutor, TabCompleter {

  private static final List<String> TAB_COMPLETE = List.of("dump");

  private final Orebfuscator orebfuscator;

  public OrebfuscatorCommand(Orebfuscator orebfuscator) {
    this.orebfuscator = orebfuscator;
  }

  @Override
  public boolean onCommand(@NonNull CommandSender sender, Command command, @NonNull String label, String[] args) {
    if (!command.getName().equalsIgnoreCase("orebfuscator")) {
      sender.sendMessage("Incorrect command registered!");
      return false;
    }

    if (!PermissionUtil.hasPermission(sender, PermissionRequirements.ADMIN)) {
      sender.sendMessage("You don't have the 'orebfuscator.admin' permission.");
      return false;
    }

    if (args.length == 0) {
      sender.sendMessage(
          "You are using %s %s".formatted(this.orebfuscator.name(), this.orebfuscator.orebfuscatorVersion()));
      sender.sendMessage(this.orebfuscator.statisticsRegistry().format());
    } else if (args[0].equalsIgnoreCase("dump")) {
      var dumpFile = new OrebfuscatorDumpFile(this.orebfuscator);

      dumpFile.set("versions.nms", MinecraftVersion.nmsVersion());
      dumpFile.set("versions.server", Bukkit.getVersion());
      dumpFile.set("versions.bukkit", Bukkit.getBukkitVersion());
      dumpFile.set("versions.protocolLib", ProtocolLibrary.getPlugin().toString());

      var plugins = dumpFile.createSection("plugins");
      for (Plugin bukkitPlugin : Bukkit.getPluginManager().getPlugins()) {
        PluginDescriptionFile description = bukkitPlugin.getDescription();

        var plugin = plugins.createSection(bukkitPlugin.getName());
        plugin.set("version", description.getVersion());
        plugin.set("author", description.getAuthors().toString());
      }

      var listeners = dumpFile.createSection("listeners");
      for (PacketListener packetListener : ProtocolLibrary.getProtocolManager().getPacketListeners()) {
        var listener = listeners.createSection(
            packetListener.getClass().toGenericString() + "@" + System.identityHashCode(packetListener));
        listener.set("plugin", packetListener.getPlugin().toString());
        listener.set("receivingWhitelist", packetListener.getReceivingWhitelist().toString());
        listener.set("sendingWhitelist", packetListener.getSendingWhitelist().toString());
      }

      Path path = dumpFile.write();
      sender.sendMessage("Dump file created at: " + path);
    } else {
      return false;
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(@NonNull CommandSender sender, @NonNull Command command, @NonNull String alias,
      String[] args) {
    return args.length == 1 ? TAB_COMPLETE : Collections.emptyList();
  }
}
