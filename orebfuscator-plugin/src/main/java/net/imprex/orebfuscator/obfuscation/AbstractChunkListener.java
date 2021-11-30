package net.imprex.orebfuscator.obfuscation;

import java.util.Set;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.PermissionUtil;

public abstract class AbstractChunkListener extends PacketAdapter {

	private final OrebfuscatorConfig config;
	private final ObfuscatorSystem obfuscatorSystem;

	public AbstractChunkListener(Orebfuscator orebfuscator) {
		super(orebfuscator, PacketType.Play.Server.MAP_CHUNK);
		this.config = orebfuscator.getOrebfuscatorConfig();
		this.obfuscatorSystem = orebfuscator.getObfuscatorSystem();
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		Player player = event.getPlayer();
		if (this.shouldNotObfuscate(player)) {
			this.skipChunkForProcessing(event);
			return;
		}

		ChunkStruct chunkStruct = new ChunkStruct(event.getPacket(), player.getWorld());
		if (chunkStruct.isEmpty()) {
			this.skipChunkForProcessing(event);
			return;
		}

		this.preChunkProcessing(event, chunkStruct);

		this.obfuscatorSystem.obfuscateOrUseCache(chunkStruct).thenAccept(chunk -> {
			chunkStruct.setDataBuffer(chunk.getData());

			Set<BlockPos> removedBlockEntities = chunk.getRemovedTileEntities();
			if (!removedBlockEntities.isEmpty()) {
				chunkStruct.removeBlockEntityIf(removedBlockEntities::contains);
			}

			this.postChunkProcessing(event, chunkStruct, chunk);
		});
	}

	public abstract void unregister();

	protected void skipChunkForProcessing(PacketEvent event) {
	}

	protected void preChunkProcessing(PacketEvent event, ChunkStruct struct) {
	}

	protected void postChunkProcessing(PacketEvent event, ChunkStruct struct, ObfuscatedChunk chunk) {
	}

	private boolean shouldNotObfuscate(Player player) {
		return PermissionUtil.canDeobfuscate(player) || !config.needsObfuscation(player.getWorld());
	}
}
