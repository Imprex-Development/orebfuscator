package net.imprex.orebfuscator.injector;

import java.util.ArrayDeque;
import java.util.ConcurrentModificationException;
import java.util.Queue;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketType.Protocol;
import com.comphenix.protocol.injector.packet.PacketRegistry;
import com.comphenix.protocol.utility.MinecraftReflection;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.util.concurrent.Promise;
import net.imprex.orebfuscator.util.OFCLogger;

public class AsyncOutboundPacketHandler extends ChannelOutboundHandlerAdapter {

	private final OrebfuscatorInjector injector;
	private final Queue<PendingWrite> pendingWrites = new ArrayDeque<>();

	private ChannelHandlerContext context;

	public AsyncOutboundPacketHandler(OrebfuscatorInjector injector) {
		this.injector = injector;
	}

	@Override
	public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
		this.context = ctx;
	}

	@Override
	public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
		Promise<Object> task = null;
		
		PacketType packetType = getPacketType(msg);
		if (packetType != null && this.injector.hasOutboundAsyncListener(packetType)) {
			// process packet async if we have any listeners
			EventLoop eventLoop = ctx.channel().eventLoop();
			task = this.injector.processOutboundAsync(packetType, msg, eventLoop);

			if (task != null) {
				// we can just call flush on completion as netty calls the listener on the
				// channel promise (channel) event-loop
				task.addListener(future -> this.flushWriteQueue());
			}
		}

		if (task != null || !this.pendingWrites.isEmpty()) {
			// we also need to delay any other tasks as the en-/decoder is configured by
			// a runnable that is written to the channels pipeline
			this.pendingWrites.offer(new PendingWrite(msg, promise, task));
		} else {
			// write if we don't wait on any previous message
			ctx.write(msg, promise);
		}
	}

	private PacketType getPacketType(Object msg) {
		if (!MinecraftReflection.isPacketClass(msg)) {
			return null;
		}

		PacketType.Protocol protocol = this.injector.getOutboundProtocol();
		if (protocol == Protocol.UNKNOWN) {
			OFCLogger.debug("skipping unknown outbound protocol for " + msg.getClass());
			return null;
		}

		PacketType packetType = PacketRegistry.getPacketType(protocol, msg.getClass());
		if (packetType == null) {
			OFCLogger.debug("skipping unknown outbound packet type for " + msg.getClass());
			return null;
		}

		return packetType;
	}

	private void flushWriteQueue() {
		if (!this.context.executor().inEventLoop()) {
			this.context.executor().execute(this::flushWriteQueue);
			return;
		}

		while (!this.pendingWrites.isEmpty()) {
			PendingWrite head = this.pendingWrites.peek();
			if (!head.isDone()) {
				return;
			}

			if (this.pendingWrites.poll() != head) {
				// paranoia check; this should never happen
				throw new ConcurrentModificationException();
			}

			head.write();
		}
	}

	private class PendingWrite {

		private final long timestamp = System.nanoTime();

		private Object message;
		private final ChannelPromise promise;
		private final Promise<Object> task;

		public PendingWrite(Object message, ChannelPromise promise, Promise<Object> task) {
			this.message = message;
			this.promise = promise;
			this.task = task;
		}

		public boolean isDone() {
			return task == null || task.isDone();
		}

		public void write() {
			if (task != null) {
				// packet got cancel; don't write anything
				if (task.isCancelled()) {
					return;
				} else if (task.cause() != null) {
					OFCLogger.error("An unknown error occurred while processing outbound packet async: "
							+ this.message.getClass(), task.cause());
				} else {
					Object message = task.getNow();
					if (message != null) {
						this.message = message;
					} else {
						OFCLogger.warn("Async packet processing returned NULL, that shouldn't happen" + task);
					}
				}
			}

			injector.logPacketDelay(System.nanoTime() - timestamp);

			if (!context.isRemoved() && context.channel().isOpen()) {
				context.write(this.message, this.promise);
			}
		}
	}
}
