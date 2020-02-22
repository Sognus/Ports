package net.robinjam.bukkit.ports.persistence;

import java.util.HashMap;
import java.util.Map;


import org.bukkit.configuration.serialization.ConfigurationSerializable;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;

/**
 * Handles serializing and deserializing WorldEdit cuboid regions to/from configuration files.
 * 
 * @author robinjam
 * @author Sognus
 */
public class PersistentCuboidRegion implements ConfigurationSerializable {
	
	private CuboidRegion region;
	
	public PersistentCuboidRegion(CuboidRegion region) {
		this.region = region;
	}

	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> result = new HashMap<String, Object>();
		result.put("x1", (double)(region.getPos1().getX()));
		result.put("y1", (double)(region.getPos1().getY()));
		result.put("z1", (double)(region.getPos1().getZ()));
		result.put("x2", (double)(region.getPos2().getX()));
		result.put("y2", (double)(region.getPos2().getY()));
		result.put("z2", (double)(region.getPos2().getZ()));
		return result;
	}
	
	public static PersistentCuboidRegion deserialize(Map<String, Object> data) {
		Double x1 = (Double)(data.get("x1"));
		Double y1 = (Double)(data.get("y1"));
		Double z1 = (Double)(data.get("z1"));
		Double x2 = (Double)(data.get("x2"));
		Double y2 = (Double)(data.get("y2"));
		Double z2 = (Double)(data.get("z2"));
		BlockVector3 pos1 = BlockVector3.at(x1, y1, z1);
		BlockVector3 pos2 = BlockVector3.at(x2, y2, z2);
		return new PersistentCuboidRegion(new CuboidRegion(pos1, pos2));
	}
	
	public CuboidRegion getRegion() {
		return region;
	}

}
