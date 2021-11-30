package net.imprex.orebfuscator.nms.v1_18_R1;

import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;

import net.imprex.orebfuscator.nms.AbstractBlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

public class BlockStateWrapper extends AbstractBlockState<BlockState> {

	public BlockStateWrapper(int x, int y, int z, World world, BlockState state) {
		super(x, y, z, world, state);
	}

	@Override
	public int getBlockId() {
		return NmsManager.getBlockId(this.state);
	}

	@Override
	public void notifyBlockChange() {
		ServerLevel serverLevel = ((CraftWorld) this.world).getHandle();
		serverLevel.getChunkSource().blockChanged(new BlockPos(this.getX(), this.getY(), this.getZ()));
	}
}
