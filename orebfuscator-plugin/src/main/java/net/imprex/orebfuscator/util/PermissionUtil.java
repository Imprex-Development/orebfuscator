package net.imprex.orebfuscator.util;

import org.bukkit.permissions.Permissible;
import dev.imprex.orebfuscator.PermissionRequirements;

public class PermissionUtil {

  public static boolean hasPermission(Permissible permissible, PermissionRequirements check) {
    return (check.operatorLevel().isPresent() && permissible.isOp())
        || (check.permission().isPresent() && permissible.hasPermission(check.permission().get()));
  }
}
