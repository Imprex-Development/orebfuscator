package dev.imprex.orebfuscator.util;

import dev.imprex.orebfuscator.PermissionRequirements;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class PermissionHelper {

  public static boolean has(ServerPlayer player, PermissionRequirements requirements) {
    var operatorLevel = requirements.operatorLevel();
    if (operatorLevel.isPresent()) {
      var requiredLevel = PermissionLevel.byId(operatorLevel.getAsInt());
      var playerLevel = player.level().getServer().getProfilePermissions(player.nameAndId()).level();
      if (playerLevel.isEqualOrHigherThan(requiredLevel)) {
        return true;
      }
    }

    var permission = requirements.permission();
    return permission.isPresent() && Permissions.check(player, permission.get());
  }
}
