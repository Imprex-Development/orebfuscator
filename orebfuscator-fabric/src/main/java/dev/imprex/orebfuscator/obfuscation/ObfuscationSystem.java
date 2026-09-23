package dev.imprex.orebfuscator.obfuscation;

import dev.imprex.orebfuscator.Orebfuscator;
import dev.imprex.orebfuscator.PermissionRequirements;
import dev.imprex.orebfuscator.event.DeobfuscationEvents;
import dev.imprex.orebfuscator.iterop.FabricWorldAccessor;
import dev.imprex.orebfuscator.util.BlockPos;
import dev.imprex.orebfuscator.util.PermissionHelper;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action;

public class ObfuscationSystem {

  public ObfuscationSystem(Orebfuscator orebfuscator) {
    var config = orebfuscator.config().general();
    var deobfuscationWorker = new DeobfuscationWorker(orebfuscator);

    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
      if (config.bypassNotification() && PermissionHelper.has(handler.getPlayer(), PermissionRequirements.BYPASS)) {
        handler.getPlayer().sendSystemMessage(Component.literal(
            "[§bOrebfuscator§f]§7 You bypass Orebfuscator because you have the 'orebfuscator.bypass' permission."));
      }
    });

    DeobfuscationEvents.SET_BLOCK.register((level, pos) -> {
      var world = FabricWorldAccessor.get(level);
      deobfuscationWorker.deobfuscate(world, new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
    });

    DeobfuscationEvents.DAMAGE_BLOCK.register((level, pos) -> {
      if (config.updateOnBlockDamage()) {
        var world = FabricWorldAccessor.get(level);
        deobfuscationWorker.deobfuscate(world, new BlockPos(pos.getX(), pos.getY(), pos.getZ()));
      }
    });
  }
}
