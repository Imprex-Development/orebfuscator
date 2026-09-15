package dev.imprex.orebfuscator;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.imprex.orebfuscator.logging.OfcLogger;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class OrebfuscatorCommand {

  private static LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
    return LiteralArgumentBuilder.literal(name);
  }

  public static LiteralArgumentBuilder<CommandSourceStack> create(Supplier<Orebfuscator> orebfuscator) {
    var command = new OrebfuscatorCommand(orebfuscator);

    return literal("orebfuscator").requires(
            source -> Commands.LEVEL_GAMEMASTERS.check(source.permissions()) || Permissions.check(source,
                "orebfuscator.admin")).then(literal("debug").executes(command::executeDebug))
        .then(literal("dump").executes(command::executeDump)).executes(command::execute);
  }

  private final Supplier<Orebfuscator> orebfuscator;

  public OrebfuscatorCommand(Supplier<Orebfuscator> orebfuscator) {
    this.orebfuscator = orebfuscator;
  }

  private int execute(@NotNull CommandContext<CommandSourceStack> context) {
    var source = context.getSource();
    var orebfuscator = this.orebfuscator.get();

    source.sendSystemMessage(Component.literal("You are using " + orebfuscator.toString()));
    try {

      source.sendSystemMessage(Component.literal(orebfuscator.statisticsRegistry().format()));
    } catch (Exception e) {
      OfcLogger.error(e);
    }

    return Command.SINGLE_SUCCESS;
  }

  private int executeDebug(@NotNull CommandContext<CommandSourceStack> context) {
    var orebfuscator = this.orebfuscator.get();
    try {
      String debug = "\n\n\n\n" + orebfuscator.statisticsRegistry().debug();
      context.getSource().sendSystemMessage(Component.literal(debug));
    } catch (Exception e) {
      OfcLogger.error(e);
    }

    return Command.SINGLE_SUCCESS;
  }

  private int executeDump(@NotNull CommandContext<CommandSourceStack> context) {
    var dumpFile = new OrebfuscatorDumpFile(this.orebfuscator.get());

    Path path = dumpFile.write();
    context.getSource().sendSystemMessage(Component.literal("Dump file created at: " + path));

    return Command.SINGLE_SUCCESS;
  }
}
