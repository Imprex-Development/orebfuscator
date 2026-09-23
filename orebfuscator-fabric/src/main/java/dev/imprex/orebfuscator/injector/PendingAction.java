package dev.imprex.orebfuscator.injector;

import java.util.function.Consumer;

import net.minecraft.network.Connection;

public interface PendingAction extends Consumer<Connection> {

  boolean isComplete();

}
