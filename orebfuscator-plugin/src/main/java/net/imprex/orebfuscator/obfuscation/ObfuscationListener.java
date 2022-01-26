package net.imprex.orebfuscator.obfuscation;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.async.AsyncListenerHandler;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.proximityhider.ProximityPlayerManager;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.OFCLogger;
import net.imprex.orebfuscator.util.PermissionUtil;

public class ObfuscationListener extends PacketAdapter {

	private final OrebfuscatorConfig config;
	private final ProximityPlayerManager proximityManager;
	private final ObfuscationSystem obfuscationSystem;

	private final AsynchronousManager asynchronousManager;
	private final AsyncListenerHandler asyncListenerHandler;

	public ObfuscationListener(Orebfuscator orebfuscator) {
		super(orebfuscator, PacketType.Play.Server.MAP_CHUNK);

		this.config = orebfuscator.getOrebfuscatorConfig();
		this.proximityManager = orebfuscator.getProximityHider().getPlayerManager();
		this.obfuscationSystem = orebfuscator.getObfuscationSystem();

		this.asynchronousManager = ProtocolLibrary.getProtocolManager().getAsynchronousManager();
		this.asyncListenerHandler = this.asynchronousManager.registerAsyncHandler(this);
		this.asyncListenerHandler.start(orebfuscator.getOrebfuscatorConfig().advanced().protocolLibThreads());
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		Player player = event.getPlayer();
		if (this.shouldNotObfuscate(player)) {
			this.skipChunkForProcessing(event);
			return;
		}

		ChunkStruct struct = new ChunkStruct(event.getPacket(), player.getWorld());
		if (struct.isEmpty()) {
			this.skipChunkForProcessing(event);
			return;
		}

		event.getAsyncMarker().incrementProcessingDelay();

		this.obfuscationSystem.obfuscate(struct).whenComplete((chunk, throwable) -> {
			if (throwable != null) {
				this.completeExceptionally(event, struct, throwable);
			} else if (chunk != null) {
				this.complete(event, struct, chunk);
			} else {
				this.skipChunkForProcessing(event);
				OFCLogger.warn(String.format("skipping chunk[world=%s, x=%d, z=%d] because obfuscation result is missing",
						struct.world.getName(), struct.chunkX, struct.chunkZ));
			}
		});
	}

	public void unregister() {
		this.asynchronousManager.unregisterAsyncHandler(this.asyncListenerHandler);
	}

	private boolean shouldNotObfuscate(Player player) {
		return PermissionUtil.canDeobfuscate(player) || !config.needsObfuscation(player.getWorld());
	}

	private void skipChunkForProcessing(PacketEvent event) {
		this.asynchronousManager.signalPacketTransmission(event);
	}

	private void completeExceptionally(PacketEvent event, ChunkStruct struct, Throwable throwable) {
		OFCLogger.error(String.format("An error occurred while obfuscating chunk[world=%s, x=%d, z=%d]",
				struct.world.getName(), struct.chunkX, struct.chunkZ), throwable);
		this.skipChunkForProcessing(event);
	}

	private void complete(PacketEvent event, ChunkStruct struct, ObfuscationResult chunk) {
		struct.setDataBuffer(chunk.getData());

		Set<BlockPos> blockEntities = chunk.getBlockEntities();
		if (!blockEntities.isEmpty()) {
			struct.removeBlockEntityIf(blockEntities::contains);
		}

		Player player = event.getPlayer();
		this.proximityManager.addAndLockChunk(player, struct.chunkX, struct.chunkZ, chunk.getProximityBlocks());

		Bukkit.getScheduler().runTask(this.plugin, () -> {
			this.asynchronousManager.signalPacketTransmission(event);
			this.proximityManager.unlockChunk(player, struct.chunkX, struct.chunkZ);
		});
	}
}
