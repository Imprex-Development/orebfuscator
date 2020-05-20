package net.imprex.orebfuscator.obfuscation;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.nbt.NbtBase;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.cache.ChunkCacheEntry;
import net.imprex.orebfuscator.chunk.ChunkStruct;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.proximityhider.ProximityHider;
import net.imprex.orebfuscator.util.BlockCoords;
import net.imprex.orebfuscator.util.PermissionUtil;

public class PacketListener extends PacketAdapter {

	private final ProtocolManager protocolManager;

	private final OrebfuscatorConfig config;
	private final Obfuscator obfuscator;

	private final ProximityHider proximityHider;

	public PacketListener(Orebfuscator orebfuscator) {
		super(orebfuscator, PacketType.Play.Server.MAP_CHUNK);

		this.protocolManager = ProtocolLibrary.getProtocolManager();
		this.protocolManager.addPacketListener(this);

		this.config = orebfuscator.getOrebfuscatorConfig();
		this.obfuscator = orebfuscator.getObfuscator();
		this.proximityHider = orebfuscator.getProximityHider();
	}

	public void unregister() {
		this.protocolManager.removePacketListener(this);
	}

	@Override
	public void onPacketSending(PacketEvent event) {
		Player player = event.getPlayer();
		if (PermissionUtil.canDeobfuscate(player)) {
			return;
		}

		World world = player.getWorld();
		if (!config.needsObfuscation(world)) {
			return;
		}

		PacketContainer packet = event.getPacket();
		StructureModifier<Integer> ints = packet.getIntegers();
		StructureModifier<byte[]> byteArray = packet.getByteArrays();
		StructureModifier<List<NbtBase<?>>> nbtList = packet.getListNbtModifier();

		List<NbtBase<?>> tileEntityList = nbtList.read(0);

		ChunkStruct chunkStruct = new ChunkStruct();
		chunkStruct.chunkX = ints.read(0);
		chunkStruct.chunkZ = ints.read(1);
		chunkStruct.primaryBitMask = ints.read(2);
		chunkStruct.data = byteArray.read(0);
		chunkStruct.isOverworld = world.getEnvironment() == World.Environment.NORMAL;

		ChunkCacheEntry chunkEntry = this.obfuscator.obfuscateOrUseCache(world, chunkStruct);
		if (chunkEntry != null) {
			byteArray.write(0, chunkEntry.getData());

			if (tileEntityList != null) {
				PacketListener.removeBlockEntities(tileEntityList, chunkEntry.getRemovedTileEntities());
				nbtList.write(0, tileEntityList);
			}

			this.proximityHider.addProximityBlocks(player, chunkStruct.chunkX, chunkStruct.chunkZ, chunkEntry.getProximityBlocks());
		}
	}

	private static void removeBlockEntities(List<NbtBase<?>> tileEntityList, Set<BlockCoords> removedTileEntities) {
		for (Iterator<NbtBase<?>> iterator = tileEntityList.iterator(); iterator.hasNext();) {
			NbtCompound tileEntity = (NbtCompound) iterator.next();

			int x = tileEntity.getInteger("x");
			int y = tileEntity.getInteger("y");
			int z = tileEntity.getInteger("z");

			BlockCoords position = new BlockCoords(x, y, z);
			if (removedTileEntities.contains(position)) {
				iterator.remove();
			}
		}
	}
}
