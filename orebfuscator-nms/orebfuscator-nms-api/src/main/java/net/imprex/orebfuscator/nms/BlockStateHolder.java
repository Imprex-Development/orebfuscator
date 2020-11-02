package net.imprex.orebfuscator.nms;

public interface BlockStateHolder {

	int getX();

	int getY();

	int getZ();

	int getBlockId();

	void markBlockForUpdate();
}
