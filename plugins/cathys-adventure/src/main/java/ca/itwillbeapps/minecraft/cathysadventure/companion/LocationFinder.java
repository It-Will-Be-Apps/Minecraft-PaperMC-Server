package ca.itwillbeapps.minecraft.cathysadventure.companion;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import ca.itwillbeapps.minecraft.cathysadventure.Settings;

public class LocationFinder {

    /**
     * Finds the closest suitable location for a companion from the given
     * location, if any.
     *
     * @param world The world in which to find a location
     * @param location The location near which to spawn
     * @return The identified location, or null if there is no safe location
     */
    public static Location nearLocation(World world, Location location) {
        for (int radius = 0; radius <= Settings.LOCATION_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    int x = location.getBlockX() + dx;
                    int z = location.getBlockZ() + dz;
                    int y = getSuitableGroundBlockY(world, x, location.getBlockY(), z);

                    Block block = world.getBlockAt(x, y, z);

                    if (!canCompanionSpawn(block)) {
                        continue;
                    }

                    return new Location(world, x + 0.5, y + 1, z + 0.5);
                }
            }
        }

        return null;
    }

    /**
     * Finds the highest ground block at the given X and Z coordinates.
     *
     * @param world The world in which to search
     * @param x The X coordinate
     * @param y The Y coordinate
     * @param z The Z coordinate
     * @return The Y coordinate of highest ground block within the given range
     *         at the given coordinates
     */
    private static int getSuitableGroundBlockY(World world, int x, int y, int z) {
        for (int blockY = y + Settings.LOCATION_HEIGHT_RANGE; blockY >= y - Settings.LOCATION_HEIGHT_RANGE; blockY--) {
            Block block = world.getBlockAt(x, blockY, z);

            if (isAirOrTree(block)) {
                continue;
            } else {
                return block.getY();
            }
        }

        return y;
    }

    /**
     * Verifies whether the given block is air or part of a tree.
     *
     * @param block The block to verify
     * @return Whether the block is air or a tree related block
     */
    private static boolean isAirOrTree(Block block) {
        return Tag.AIR.isTagged(block.getType())
            || Tag.LEAVES.isTagged(block.getType())
            || Tag.LOGS.isTagged(block.getType());
    }

    /**
     * Verifies if the given block is clear for spawning companions.
     *
     * @param block The block to verify
     * @return Whether the block is clear to spawn companions
     */
    private static boolean canCompanionSpawn(Block block) {
        Material groundType = block.getType();

        if (!groundType.isSolid()) {

            return false;
        }

        Block legsBlock = block.getRelative(BlockFace.UP);

        if (!legsBlock.isEmpty() && !legsBlock.isPassable()) {
            return false;
        }

        if (legsBlock.isLiquid()) {
            return false;
        }

        Block headBlock = legsBlock.getRelative(BlockFace.UP);

        if (!headBlock.isEmpty() && !headBlock.isPassable()) {
            return false;
        }

        if (headBlock.isLiquid()) {
            return false;
        }

        return true;
    }
}
