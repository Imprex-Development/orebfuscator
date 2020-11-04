package net.imprex.orebfuscator.obfuscation;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;

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

		PacketContainer packet = event.getPacket();
		ChunkStruct chunkStruct = new ChunkStruct(packet, player.getWorld());
		if (chunkStruct.isEmpty()) {
			this.skipChunkForProcessing(event);
			return;
		}

		this.preChunkProcessing(event, chunkStruct);

		this.obfuscatorSystem.obfuscateOrUseCache(chunkStruct).thenAccept(chunk -> {

			packet.getByteArrays().write(0, chunk.getData());
			this.removeTileEntitiesFromPacket(packet, chunk.getRemovedTileEntities());

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

	private void removeTileEntitiesFromPacket(PacketContainer packet, Set<BlockPos> positions) {
		if (!positions.isEmpty()) {
			StructureModifier<List<NbtBase<?>>> packetNbtList = packet.getListNbtModifier();

			List<NbtBase<?>> tileEntities = packetNbtList.read(0);
			this.removeTileEntities(tileEntities, positions);
			packetNbtList.write(0, tileEntities);
		}
	}

	private void removeTileEntities(List<NbtBase<?>> tileEntities, Set<BlockPos> positions) {
		for (Iterator<NbtBase<?>> iterator = tileEntities.iterator(); iterator.hasNext();) {
			NbtCompound tileEntity = (NbtCompound) iterator.next();

			int x = tileEntity.getInteger("x");
			int y = tileEntity.getInteger("y");
			int z = tileEntity.getInteger("z");

			BlockPos position = new BlockPos(x, y, z);
			if (positions.contains(position)) {
				iterator.remove();
			}
		}
	}
}
