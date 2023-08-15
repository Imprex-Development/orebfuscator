package net.imprex.orebfuscator;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import net.imprex.orebfuscator.util.ConsoleUtil;
import net.imprex.orebfuscator.util.JavaVersion;
import net.imprex.orebfuscator.util.PermissionUtil;
import net.md_5.bungee.api.chat.TextComponent;

public class SyncPacketListenerDeprecationNotifier implements Listener {

	private final boolean isUsingSyncListener;

	public SyncPacketListenerDeprecationNotifier(Orebfuscator orebfuscator) {
		this.isUsingSyncListener = !orebfuscator.getOrebfuscatorConfig().advanced().useAsyncPacketListener();

		Bukkit.getPluginManager().registerEvents(this, orebfuscator);

		if (isUsingSyncListener) {
			ConsoleUtil.printBox(Level.SEVERE,
					"WARNING",
					"SUPPORT FOR SYNC PACKET LISTENER WILL BE DROPPED IN THE NEAR FUTURE",
					"",
					"Future releases of Orebfuscator will only support async packet listeners");
		}
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		if (PermissionUtil.canAccessAdminTools(player) && isUsingSyncListener) {
			player.spigot().sendMessage(new TextComponent(String.format(
					"[§bOrebfuscator§f]§c§l You are using sync packet listeners which Orebfuscator will no longer support in the near future! Please make sure everything works fine with async packet listeners enabled.",
					JavaVersion.get())));
		}
	}
}
