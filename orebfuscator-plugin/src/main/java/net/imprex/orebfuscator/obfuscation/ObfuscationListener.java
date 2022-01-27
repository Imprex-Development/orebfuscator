package net.imprex.orebfuscator.obfuscation;

import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.proximityhider.ProximityPlayerManager;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.OFCLogger;
import net.imprex.orebfuscator.util.PermissionUtil;

public abstract class ObfuscationListener extends PacketAdapter {

	private final OrebfuscatorConfig config;
	private final ProximityPlayerManager proximityManager;
	private final ObfuscationSystem obfuscationSystem;

	public ObfuscationListener(Orebfuscator orebfuscator) {
		super(orebfuscator, PacketType.Play.Server.MAP_CHUNK);

		this.config = orebfuscator.getOrebfuscatorConfig();
		this.proximityManager = orebfuscator.getProximityHider().getPlayerManager();
		this.obfuscationSystem = orebfuscator.getObfuscationSystem();
	}

	protected abstract void skipChunkForProcessing(PacketEvent event);

	protected abstract void preChunkProcessing(PacketEvent event);

	protected abstract void postChunkProcessing(PacketEvent event);

	public abstract void unregister();

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

		this.preChunkProcessing(event);

		this.obfuscationSystem.obfuscate(struct).whenComplete((chunk, throwable) -> {
			if (throwable != null) {
				this.completeExceptionally(event, struct, throwable);
			} else if (chunk != null) {
				this.complete(event, struct, chunk);
			} else {
				OFCLogger.warn(String.format("skipping chunk[world=%s, x=%d, z=%d] because obfuscation result is missing",
						struct.world.getName(), struct.chunkX, struct.chunkZ));
				this.postChunkProcessing(event);
			}
		});
	}

	private boolean shouldNotObfuscate(Player player) {
		return PermissionUtil.canDeobfuscate(player) || !config.needsObfuscation(player.getWorld());
	}

	private void completeExceptionally(PacketEvent event, ChunkStruct struct, Throwable throwable) {
		OFCLogger.error(String.format("An error occurred while obfuscating chunk[world=%s, x=%d, z=%d]",
				struct.world.getName(), struct.chunkX, struct.chunkZ), throwable);
		this.postChunkProcessing(event);
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
			this.postChunkProcessing(event);
			this.proximityManager.unlockChunk(player, struct.chunkX, struct.chunkZ);
		});
	}
}
