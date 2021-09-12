package net.imprex.orebfuscator.util;

import org.bukkit.entity.Player;

public class PermissionUtil {

	public static boolean canDeobfuscate(Player player) {
		try {
			return player.hasPermission("orebfuscator.bypass");
		} catch (UnsupportedOperationException e) {
			// fix #131: catch TemporaryPlayer not implementing hasPermission
			return false;
		}
	}

	public static boolean canCheckForUpdates(Player player) {
		return player.isOp() || player.hasPermission("orebfuscator.admin");
	}
}
