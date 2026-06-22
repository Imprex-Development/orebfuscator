package net.imprex.orebfuscator.util;

import dev.imprex.orebfuscator.PermissionRequirements;
import org.bukkit.permissions.Permissible;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PermissionUtil {

  public static boolean hasPermission(Permissible permissible, PermissionRequirements check) {
    return (check.operatorLevel().isPresent() && permissible.isOp())
        || (check.permission().isPresent() && permissible.hasPermission(check.permission().get()));
  }
}
