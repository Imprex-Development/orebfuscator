package dev.imprex.orebfuscator;

import dev.imprex.orebfuscator.iterop.FabricLoggerAccessor;
import dev.imprex.orebfuscator.logging.OfcLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

public class OrebfuscatorFabric implements ModInitializer {

  private Orebfuscator orebfuscator;

  @Override
  public void onInitialize() {
    ModContainer modContainer = FabricLoader.getInstance().getModContainer("orebfuscator-fabric").orElseThrow();

    CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> {
      dispatcher.register(OrebfuscatorCommand.create(() -> this.orebfuscator));
    });

    ServerLifecycleEvents.SERVER_STARTING.register(server -> {
      OfcLogger.setLogger(new FabricLoggerAccessor());
    });

    ServerLifecycleEvents.SERVER_STARTED.register(server -> {
      orebfuscator = new Orebfuscator(server, modContainer);
    });
  }
}
