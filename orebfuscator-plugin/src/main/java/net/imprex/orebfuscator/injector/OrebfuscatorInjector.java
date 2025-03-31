package net.imprex.orebfuscator.injector;

import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.fuzzy.FuzzyFieldContract;
import com.comphenix.protocol.utility.MinecraftFields;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.util.concurrent.Promise;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.AdvancedConfig;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.obfuscation.ObfuscationResult;
import net.imprex.orebfuscator.obfuscation.ObfuscationSystem;
import net.imprex.orebfuscator.player.OrebfuscatorPlayer;
import net.imprex.orebfuscator.player.OrebfuscatorPlayerMap;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.OFCLogger;
import net.imprex.orebfuscator.util.PermissionUtil;

public class OrebfuscatorInjector {

	private static final String OUTBOUND_HANDLER_NAME = "orebfuscator_outbound";

	private final OrebfuscatorConfig config;
	private final OrebfuscatorPlayerMap playerMap;
	private final ObfuscationSystem obfuscationSystem;

	private final Player player;
	private final Channel channel;

	private final Queue<Long> delayPackets = new LinkedList<Long>();

	public OrebfuscatorInjector(Orebfuscator orebfuscator, Player player) {
		this.config = orebfuscator.getOrebfuscatorConfig();
		this.playerMap = orebfuscator.getPlayerMap();
		this.obfuscationSystem = orebfuscator.getObfuscationSystem();

		Object networkManager = MinecraftFields.getNetworkManager(player);
		var channelAccessor = Accessors.getFieldAccessor(FuzzyReflection.fromObject(networkManager, true)
				.getField(FuzzyFieldContract.newBuilder()
				.typeExact(Channel.class)
				.banModifier(Modifier.STATIC)
				.build()));

		this.player = player;
		this.channel = (Channel) channelAccessor.get(networkManager);

		ChannelPipeline pipeline = this.channel.pipeline();

		String encoderName = pipeline.get("outbound_config") != null
				? "outbound_config"
				: "encoder";

		if (pipeline.context(OUTBOUND_HANDLER_NAME) == null) {
			pipeline.addAfter(encoderName, OUTBOUND_HANDLER_NAME, new AsyncOutboundPacketHandler(this));
		}
	}

	public void uninject() {
		if (!this.channel.eventLoop().inEventLoop()) {
			channel.eventLoop().execute(this::uninject);
			return;
		}

		ChannelPipeline pipeline = this.channel.pipeline();
		if (pipeline.context(OUTBOUND_HANDLER_NAME) != null) {
			pipeline.remove(OUTBOUND_HANDLER_NAME);
		}
	}

	public void logPacketDelay(long nanos) {
		while (this.delayPackets.size() > 10_000)
			this.delayPackets.poll();
		
		this.delayPackets.offer(nanos);
	}

	public double averagePacketDelay() {
		return this.delayPackets.stream()
				.mapToLong(Long::longValue)
				.average()
				.orElse(0d);
	}

	public PacketType.Protocol getOutboundProtocol() {
		return ChannelProtocolUtil.PROTOCOL_RESOLVER.apply(this.channel, PacketType.Sender.SERVER);
	}

	public boolean hasOutboundAsyncListener(PacketType packetType) {
		return packetType == PacketType.Play.Server.MAP_CHUNK;
	}

	public void processOutboundAsync(PacketType packetType, Object packet, Promise<Object> promise) {
		if (this.shouldNotObfuscate(this.player)) {
			return;
		}

		PacketContainer packetContainer = new PacketContainer(packetType, packet);
		ChunkStruct struct = new ChunkStruct(packetContainer, this.player.getWorld());
		if (struct.isEmpty()) {
			return;
		}

		CompletableFuture<ObfuscationResult> future = this.obfuscationSystem.obfuscate(struct);

		AdvancedConfig advancedConfig = this.config.advanced();
		if (advancedConfig.hasObfuscationTimeout()) {
			future = future.orTimeout(advancedConfig.obfuscationTimeout(), TimeUnit.MILLISECONDS);
		}

		future.whenComplete((chunk, throwable) -> {
			if (throwable != null) {
				this.completeExceptionally(struct, throwable);
				promise.setSuccess(packet);
			} else if (chunk != null) {
				this.complete(struct, chunk);
				promise.setSuccess(packet);
			} else {
				OFCLogger.warn(String.format("skipping chunk[world=%s, x=%d, z=%d] because obfuscation result is missing",
						struct.world.getName(), struct.chunkX, struct.chunkZ));
				promise.setSuccess(packet);
			}
		});
	}

	private boolean shouldNotObfuscate(Player player) {
		return PermissionUtil.canBypassObfuscate(player) || !config.world(player.getWorld()).needsObfuscation();
	}

	private void completeExceptionally(ChunkStruct struct, Throwable throwable) {
		if (throwable instanceof TimeoutException) {
			OFCLogger.warn(String.format("Obfuscation for chunk[world=%s, x=%d, z=%d] timed out",
					struct.world.getName(), struct.chunkX, struct.chunkZ));
		} else {
			OFCLogger.error(String.format("An error occurred while obfuscating chunk[world=%s, x=%d, z=%d]",
					struct.world.getName(), struct.chunkX, struct.chunkZ), throwable);
		}
	}

	private void complete(ChunkStruct struct, ObfuscationResult chunk) {
		struct.setDataBuffer(chunk.getData());

		Set<BlockPos> blockEntities = chunk.getBlockEntities();
		if (!blockEntities.isEmpty()) {
			struct.removeBlockEntityIf(blockEntities::contains);
		}

		final OrebfuscatorPlayer orebfuscatorPlayer = this.playerMap.get(this.player);
		if (orebfuscatorPlayer != null) {
			orebfuscatorPlayer.addChunk(struct.chunkX, struct.chunkZ, chunk.getProximityBlocks());
		}
	}
}
