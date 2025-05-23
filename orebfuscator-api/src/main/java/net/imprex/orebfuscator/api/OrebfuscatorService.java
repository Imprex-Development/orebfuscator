package net.imprex.orebfuscator.api;

import java.util.Collection;

import org.bukkit.block.Block;

/**
 * <p>
 * Service that gives access to some of Orebfuscators internal methods. Use with
 * caution.
 * </p>
 * 
 * <p>
 * All calls to this service are expected to originate from the servers
 * main-thread or in case of folia from a region thread (never the global region thread).
 * </p>
 * 
 * @since 5.2.0
 */
public interface OrebfuscatorService {

	/**
	 * Deobfuscates a list of blocks. All blocks are expected to be located in the
	 * same world. It is recommended to call this method after the block update.
	 * 
	 * @param blocks list of blocks to deobfuscate
	 */
	void deobfuscate(Collection<? extends Block> blocks);
}
