package dev.imprex.orebfuscator.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.imprex.orebfuscator.Orebfuscator;
import dev.imprex.orebfuscator.event.ConnectionEvents;
import dev.imprex.orebfuscator.injector.ChunkBatchAction;
import dev.imprex.orebfuscator.injector.PacketSendAction;
import dev.imprex.orebfuscator.injector.PendingAction;
import dev.imprex.orebfuscator.logging.OfcLogger;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;

import java.util.Queue;
import java.util.function.Consumer;

import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket;
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket;
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {

  @Shadow
  @Final
  private Queue<Consumer<Connection>> pendingActions;

  @Unique
  private ChunkBatchAction orebfuscator$pendingChunkBatch;

  @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
      at = @At(value = "INVOKE",
          target = "Lnet/minecraft/network/Connection;genericsFtw(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;)V"))
  public void orebfuscator$genericsFtw(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci,
      @Local(name = "packetListener") PacketListener packetListener) {
    ConnectionEvents.RECEIVE.invoker().onPacketReceive((Connection) (Object) this, packet, packetListener);
  }

  @Redirect(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
      at = @At(value = "INVOKE",
          target = "Lnet/minecraft/network/Connection;sendPacket(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V"))
  public void orebfuscator$send(Connection instance, Packet<?> packet, ChannelFutureListener listener, boolean flush) {
    var future = ConnectionEvents.SEND.invoker().onPacketSend((Connection) (Object) this, packet);

    if (packet instanceof ClientboundChunkBatchStartPacket) {
      if (this.orebfuscator$pendingChunkBatch != null) {
        this.orebfuscator$pendingChunkBatch.finish();
        OfcLogger.warn("Pending chunk batch discarded because a new batch was initiated.");
      }
      this.orebfuscator$pendingChunkBatch = new ChunkBatchAction();
      this.pendingActions.add(this.orebfuscator$pendingChunkBatch);
      return;
    } else if (this.orebfuscator$pendingChunkBatch != null && packet instanceof ClientboundLevelChunkWithLightPacket) {
      this.orebfuscator$pendingChunkBatch.addChunk(packet, listener, future);
      return;
    } else if (this.orebfuscator$pendingChunkBatch != null && packet instanceof ClientboundChunkBatchFinishedPacket) {
      this.orebfuscator$pendingChunkBatch.finish();
      this.orebfuscator$pendingChunkBatch = null;
      return;
    }

    if (future != null || !this.pendingActions.isEmpty()) {
      this.pendingActions.add(new PacketSendAction(packet, listener, flush, future));
    } else {
      instance.sendPacket(packet, listener, flush);
      Orebfuscator.injectorStatistics.packetDelayAny.add(0L);
    }
  }

  @WrapOperation(method = "flushQueue",
      at = @At(value = "INVOKE", target = "Ljava/util/Queue;poll()Ljava/lang/Object;"))
  public <E> E orebfuscator$flushQueue(Queue<E> instance, Operation<E> original) {
    var action = instance.peek();

    if (action instanceof PendingAction pendingAction && !pendingAction.isComplete()) {
      return null;
    }

    return instance.poll();
  }
}
