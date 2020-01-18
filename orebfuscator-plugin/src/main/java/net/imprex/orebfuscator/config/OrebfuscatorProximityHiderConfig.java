package net.imprex.orebfuscator.config;

import java.util.Set;

import org.bukkit.Material;

public class OrebfuscatorProximityHiderConfig implements ProximityHiderConfig {

	private boolean enabled;
	private int distance;
	private int distanceSquared;
	private Set<Integer> hiddenBlocks;

	@Override
	public boolean enabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int distance() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int distanceSquared() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Material randomMaterial() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Integer> hiddenBlocks() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean shouldHide(int y, int id) {
		// TODO Auto-generated method stub
		return false;
	}
}
