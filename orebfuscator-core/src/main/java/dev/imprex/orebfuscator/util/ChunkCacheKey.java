package dev.imprex.orebfuscator.util;

import org.jetbrains.annotations.NotNull;
import dev.imprex.orebfuscator.interop.WorldAccessor;

public record ChunkCacheKey(@NotNull String world, int x, int z) {

	public ChunkCacheKey(@NotNull WorldAccessor world, BlockPos position) {
		this(world.getName(), position.x >> 4, position.z >> 4);
	}

    public ChunkCacheKey(@NotNull WorldAccessor world, int x, int z) {
        this(world.getName(), x, z);
    }
}
