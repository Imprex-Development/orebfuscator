package net.imprex.orebfuscator.obfuscation;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.comphenix.protocol.AsynchronousManager;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.async.AsyncListenerHandler;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.proximityhider.ProximityPlayerManager;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.PermissionUtil;

public class PacketListener extends PacketAdapter {

	private final ProtocolManager protocolManager;
	private final AsynchronousManager asynchronousManager;
	private final AsyncListenerHandler asyncListenerHandler;
	private final boolean async;

	private final OrebfuscatorConfig config;
	private final Obfuscator obfuscator;

	private final ProximityPlayerManager proximityChunkManager;

	public PacketListener(Orebfuscator orebfuscator) {
		super(orebfuscator, PacketType.Play.Server.MAP_CHUNK);

		this.protocolManager = ProtocolLibrary.getProtocolManager();
		this.asynchronousManager = this.protocolManager.getAsynchronousManager();

		if (orebfuscator.getOrebfuscatorConfig().cache().enabled()) {
			this.asyncListenerHandler = this.asynchronousManager.registerAsyncHandler(this);
			this.asyncListenerHandler.start(orebfuscator.getOrebfuscatorConfig().cache().protocolLibThreads());
			this.async = true;
		} else {
			this.async = false;
			this.asyncListenerHandler = null;
			this.protocolManager.addPacketListener(this);
		}

		this.config = orebfuscator.getOrebfuscatorConfig();
		this.obfuscator = orebfuscator.getObfuscator();
		this.proximityChunkManager = orebfuscator.getProximityHider().getPlayerManager();
	}

	public void unregister() {
		if (this.asyncListenerHandler != null) {
			this.asynchronousManager.unregisterAsyncHandler(this.asyncListenerHandler);
		} else {
			this.protocolManager.removePacketListener(this);
		}
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		Player player = event.getPlayer();
		World world = player.getWorld();
		if (PermissionUtil.canDeobfuscate(player) || !config.needsObfuscation(world)) {
			return;
		}

		PacketContainer packet = event.getPacket();
		ChunkStruct chunkStruct = ChunkStruct.from(packet, world);
		if (chunkStruct.isEmpty()) {
			return;
		}

		if (this.async) {
			event.getAsyncMarker().incrementProcessingDelay();
		}

		this.obfuscator.obfuscateOrUseCache(world, chunkStruct).thenAccept(chunk -> {

			packet.getByteArrays().write(0, chunk.getData());
			this.removeTileEntitiesFromPacket(packet, chunk.getRemovedTileEntities());

			this.proximityChunkManager.addAndLockChunk(player, chunkStruct.chunkX, chunkStruct.chunkZ,
					chunk.getProximityBlocks());

			if (this.async) {
				Bukkit.getScheduler().runTask(this.plugin, () -> {
					this.asynchronousManager.signalPacketTransmission(event);
					Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
						this.proximityChunkManager.unlockChunk(player, chunkStruct.chunkX, chunkStruct.chunkZ);
					}, 1L);
				});
			} else {
				this.proximityChunkManager.unlockChunk(player, chunkStruct.chunkX, chunkStruct.chunkZ);
			}
		});
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
