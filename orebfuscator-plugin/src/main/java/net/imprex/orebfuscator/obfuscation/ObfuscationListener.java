package net.imprex.orebfuscator.obfuscation;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;

import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.PacketTypeEnum;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.async.AsyncListenerHandler;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import dev.imprex.orebfuscator.config.OrebfuscatorConfig;
import dev.imprex.orebfuscator.config.api.AdvancedConfig;
import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorCompatibility;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.iterop.BukkitWorldAccessor;
import net.imprex.orebfuscator.player.OrebfuscatorPlayer;
import net.imprex.orebfuscator.player.OrebfuscatorPlayerMap;
import dev.imprex.orebfuscator.logging.OfcLogger;
import dev.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.PermissionUtil;
import net.imprex.orebfuscator.util.RingLongBuffer;
import net.imprex.orebfuscator.util.ServerVersion;

public class ObfuscationListener extends PacketAdapter {

	private static final List<PacketType> PACKET_TYPES = Arrays.asList(
		PacketType.Play.Server.MAP_CHUNK,
		PacketType.Play.Server.UNLOAD_CHUNK,
		PacketType.Play.Server.LIGHT_UPDATE,
		PacketType.Play.Server.TILE_ENTITY_DATA,
		tryGetPacketType(PacketType.Play.Client.getInstance(), "CHUNK_BATCH_RECEIVED")
	);

	private static PacketType tryGetPacketType(PacketTypeEnum packetTypeEnum, String name) {
		return packetTypeEnum.values().stream()
			.filter(packetType -> packetType.name().equals(name))
			.findAny()
			.orElse(null);
	}

	private final OrebfuscatorConfig config;
	private final OrebfuscatorPlayerMap playerMap;
	private final ObfuscationSystem obfuscationSystem;

	private final AsynchronousManager asynchronousManager;
	private final AsyncListenerHandler asyncListenerHandler;

	private final RingLongBuffer originalSize = new RingLongBuffer(1000);
	private final RingLongBuffer obfuscatedSize = new RingLongBuffer(1000);

	public ObfuscationListener(Orebfuscator orebfuscator) {
		super(orebfuscator, PACKET_TYPES.stream()
				.filter(Objects::nonNull)
				.filter(PacketType::isSupported)
				.collect(Collectors.toList()));

		this.config = orebfuscator.getOrebfuscatorConfig();
		this.playerMap = orebfuscator.getPlayerMap();
		this.obfuscationSystem = orebfuscator.getObfuscationSystem();

		this.asynchronousManager = ProtocolLibrary.getProtocolManager().getAsynchronousManager();
		this.asyncListenerHandler = this.asynchronousManager.registerAsyncHandler(this);

		if (ServerVersion.isFolia()) {
			OrebfuscatorCompatibility.runAsyncNow(this.asyncListenerHandler.getListenerLoop());
		} else {
			this.asyncListenerHandler.start();
		}

		var statistics = orebfuscator.getStatistics();
		statistics.setOriginalChunkSize(() -> (long) originalSize.average());
		statistics.setObfuscatedChunkSize(() -> (long) obfuscatedSize.average());
	}

	public void unregister() {
		this.asynchronousManager.unregisterAsyncHandler(this.asyncListenerHandler);
	}

	@Override
	public void onPacketReceiving(PacketEvent event) {
		event.getPacket().getFloat().write(0, 10f);
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		if (event.getPacket().getType() != PacketType.Play.Server.MAP_CHUNK) {
			return;
		}

		Player player = event.getPlayer();
		BukkitWorldAccessor worldAccessor = BukkitWorldAccessor.get(player.getWorld());
		if (this.shouldNotObfuscate(player, worldAccessor)) {
			return;
		}

		ChunkStruct struct = new ChunkStruct(event.getPacket(), worldAccessor);
		if (struct.isEmpty()) {
			return;
		}

		// delay packet
		event.getAsyncMarker().incrementProcessingDelay();

		CompletableFuture<ObfuscationResult> future = this.obfuscationSystem.obfuscate(struct);

		AdvancedConfig advancedConfig = this.config.advanced();
		if (advancedConfig.hasObfuscationTimeout()) {
			future = future.orTimeout(advancedConfig.obfuscationTimeout(), TimeUnit.MILLISECONDS);
		}

		future.whenComplete((chunk, throwable) -> {
			if (throwable != null) {
				this.completeExceptionally(event, struct, throwable);
			} else if (chunk != null) {
				this.complete(event, struct, chunk);
			} else {
				OfcLogger.warn(String.format("skipping chunk[world=%s, x=%d, z=%d] because obfuscation result is missing",
						struct.worldAccessor.getName(), struct.chunkX, struct.chunkZ));
				this.asynchronousManager.signalPacketTransmission(event);
			}
		});
	}

	private boolean shouldNotObfuscate(Player player, BukkitWorldAccessor worldAccessor) {
		return PermissionUtil.canBypassObfuscate(player) || !config.world(worldAccessor).needsObfuscation();
	}

	private void completeExceptionally(PacketEvent event, ChunkStruct struct, Throwable throwable) {
		if (throwable instanceof TimeoutException) {
			OfcLogger.warn(String.format("Obfuscation for chunk[world=%s, x=%d, z=%d] timed out",
					struct.worldAccessor.getName(), struct.chunkX, struct.chunkZ));
		} else {
			OfcLogger.error(String.format("An error occurred while obfuscating chunk[world=%s, x=%d, z=%d]",
					struct.worldAccessor.getName(), struct.chunkX, struct.chunkZ), throwable);
		}

		this.asynchronousManager.signalPacketTransmission(event);
	}

	private void complete(PacketEvent event, ChunkStruct struct, ObfuscationResult chunk) {
		originalSize.add(struct.data.length);
		obfuscatedSize.add(chunk.getData().length);

		struct.setDataBuffer(chunk.getData());

		Set<BlockPos> blockEntities = chunk.getBlockEntities();
		if (!blockEntities.isEmpty()) {
			struct.removeBlockEntityIf(blockEntities::contains);
		}

		final OrebfuscatorPlayer player = this.playerMap.get(event.getPlayer());
		if (player != null) {
			player.addChunk(struct.chunkX, struct.chunkZ, chunk.getProximityBlocks());
		}

		this.asynchronousManager.signalPacketTransmission(event);
	}
}
