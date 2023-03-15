package net.imprex.orebfuscator;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

import net.imprex.orebfuscator.util.ConsoleUtil;
import net.imprex.orebfuscator.util.JavaVersion;
import net.imprex.orebfuscator.util.PermissionUtil;
import net.md_5.bungee.api.chat.TextComponent;

public class JavaVersionNotifier implements Listener {

	private final boolean isVersionSupported = JavaVersion.get() >= 17;

	public JavaVersionNotifier(Plugin plugin) {
		Bukkit.getPluginManager().registerEvents(this, plugin);

		if (!isVersionSupported) {
			ConsoleUtil.printBox(Level.SEVERE,
					"WARNING",
					String.format("SUPPORT FOR JAVA %s WILL BE DROPPED IN THE NEAR FUTURE", JavaVersion.get()),
					"",
					"Future release of Orebfuscator will only support Java 17+");
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (PermissionUtil.canAccessAdminTools(player) && !isVersionSupported) {
			player.spigot().sendMessage(new TextComponent(String.format(
					"[§bOrebfuscator§f]§c§l This servers java version (%s) will no longer be supported by Orebfuscator in the near future! Please update to Java 17+.",
					JavaVersion.get())));
		}
	}
}
