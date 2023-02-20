package net.imprex.orebfuscator.nms.v1_16_R3;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;

import net.imprex.orebfuscator.nms.AbstractBlockState;
import net.minecraft.server.v1_16_R3.Block;
import net.minecraft.server.v1_16_R3.BlockPosition;
import net.minecraft.server.v1_16_R3.IBlockData;
import net.minecraft.server.v1_16_R3.WorldServer;

public class BlockState extends AbstractBlockState<IBlockData> {

	public BlockState(int x, int y, int z, World world, IBlockData state) {
		super(x, y, z, world, state);
	}

	@Override
	public int getBlockId() {
		return Block.getCombinedId(this.state);
	}

	@Override
	public void notifyBlockChange() {
		WorldServer worldServer = ((CraftWorld) this.world).getHandle();
		worldServer.getChunkProvider().flagDirty(new BlockPosition(this.getX(), this.getY(), this.getZ()));
	}
}
