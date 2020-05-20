package net.imprex.orebfuscator.util;

import org.bukkit.entity.Player;

public class PermissionUtil {

	public static boolean canDeobfuscate(Player player) {
		return false;/* TODO player.isOp() || player.hasPermission("orebfuscator.bypass");*/
	}
}
