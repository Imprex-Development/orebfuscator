package net.imprex.orebfuscator;

import java.util.Collection;
import java.util.Iterator;

import org.bukkit.World;
import org.bukkit.block.Block;

import net.imprex.orebfuscator.api.OrebfuscatorService;
import net.imprex.orebfuscator.obfuscation.ObfuscatorSystem;

public final class DefaultOrebfuscatorService implements OrebfuscatorService {

	private final Orebfuscator orebfuscator;
	private final ObfuscatorSystem obfuscatorSystem;

	public DefaultOrebfuscatorService(Orebfuscator orebfuscator) {
		this.orebfuscator = orebfuscator;
		this.obfuscatorSystem = orebfuscator.getObfuscatorSystem();
	}

	@Override
	public final void deobfuscate(Collection<? extends Block> blocks) {
		if (!this.orebfuscator.isMainThread()) {
            throw new IllegalStateException("Asynchronous deobfuscation!");
		} else if (blocks == null || blocks.isEmpty()) {
			throw new IllegalArgumentException("block list is null or empty");
		}

		Iterator<? extends Block> blockIterator = blocks.iterator();
		World world = blockIterator.next().getWorld();
		while (blockIterator.hasNext()) {
			if (blockIterator.next().getWorld() != world) {
				throw new IllegalArgumentException("block list is located in more than one world");
			}
		}

		this.obfuscatorSystem.deobfuscate(blocks);
	}
}
