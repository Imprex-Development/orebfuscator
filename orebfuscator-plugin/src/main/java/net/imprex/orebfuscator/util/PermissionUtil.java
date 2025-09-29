package net.imprex.orebfuscator.util;

import org.bukkit.permissions.Permissible;

public class PermissionUtil {

  public static boolean canBypassObfuscate(Permissible permissible) {
    try {
      return permissible.hasPermission("orebfuscator.bypass");
    } catch (UnsupportedOperationException e) {
      // fix #131: catch TemporaryPlayer not implementing hasPermission
      return false;
    }
  }

  public static boolean canAccessAdminTools(Permissible permissible) {
    return permissible.isOp() || permissible.hasPermission("orebfuscator.admin");
  }
}
