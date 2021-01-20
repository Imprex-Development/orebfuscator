package net.imprex.orebfuscator.nms;

import org.bukkit.World;

import net.imprex.orebfuscator.util.BlockPos;

public interface BlockStateHolder {

	World getWorld();
	BlockPos getPosition();

	int getX();
	int getY();
	int getZ();

	int getBlockId();
	void notifyBlockChange();
}
