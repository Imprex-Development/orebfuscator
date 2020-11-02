package net.imprex.orebfuscator.nms.v1_13_R1;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R1.CraftWorld;

import net.imprex.orebfuscator.nms.AbstractBlockState;
import net.minecraft.server.v1_13_R1.IBlockData;
import net.minecraft.server.v1_13_R1.PlayerChunk;
import net.minecraft.server.v1_13_R1.PlayerChunkMap;
import net.minecraft.server.v1_13_R1.WorldServer;

public class BlockState extends AbstractBlockState<IBlockData> {

	public BlockState(int x, int y, int z, World world, IBlockData state) {
		super(x, y, z, world, state);
	}

	@Override
	public int getBlockId() {
		return NmsManager.getBlockId(this.state);
	}

	@Override
	public void markBlockForUpdate() {
		WorldServer worldServer = ((CraftWorld) this.world).getHandle();
		PlayerChunkMap chunkMap = worldServer.getPlayerChunkMap();
		PlayerChunk playerChunk = chunkMap.getChunk(this.x >> 4, this.z >> 4);
		if (playerChunk != null) {
			playerChunk.a(this.x & 15, this.y & 15, this.z & 15);
		}
	}
}
