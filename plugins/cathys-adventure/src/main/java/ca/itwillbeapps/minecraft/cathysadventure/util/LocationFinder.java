package ca.itwillbeapps.minecraft.cathysadventure.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.BiomeSearchResult;

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
    public static Location nearLocation(Location location) {
        World world = location.getWorld();

        for (int radius = 0; radius <= Settings.LOCATION_SEARCH_RADIUS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }

                    int x = location.getBlockX() + dx;
                    int z = location.getBlockZ() + dz;
                    int y = getSuitableGroundBlockY(world, x, location.getBlockY(), z, Settings.LOCATION_HEIGHT_RANGE);

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
     * @param maxHeightRange The max height to explore above the given Y
     * @return The Y coordinate of highest ground block within the given range
     *         at the given coordinates
     */
    private static int getSuitableGroundBlockY(World world, int x, int y, int z, int maxHeightRange) {
        for (int blockY = y + maxHeightRange; blockY >= y - maxHeightRange; blockY--) {
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

    /**
     * Finds a location in or near the center of the given biome type.
     *
     * @param origin The origin point of the biome search
     * @param biome The type of biome to search for
     * @return The identified location, or null if there is no safe location
     */
    public static Location inBiome(Location origin, Biome biome) {
        World world = origin.getWorld();
        BiomeSearchResult biomeResult = world.locateNearestBiome(origin, Settings.BIOME_SEARCH_RADIUS, biome);

        if (biomeResult != null) {
            Location biomeLocation = biomeResult.getLocation();
            Location biomeCenterLocation = findBiomeCenter(biomeLocation);
            return nearLocation(biomeCenterLocation);
        }

        return null;
    }

    /**
     * Finds the approximate center of the biome at the given location.
     *
     * @param location A known location inside the biome
     * @return The approximate center location of the biome
     */
    private static Location findBiomeCenter(Location location) {
        World world = location.getWorld();

        if (world == null) {
            return location;
        }

        Biome biome = location.getBlock().getBiome();
        int startX = location.getBlockX();
        int startZ = location.getBlockZ();
        int y = location.getBlockY();
        int stepSize = 20;
        int maxRadius = 500;

        int xMin = startX;
        for (int x = startX; x >= startX - maxRadius; x -= stepSize) {
            if (world.getBiome(x, y, startZ) != biome) {
                xMin = x + stepSize;
                break;
            }
        }

        int xMax = startX;
        for (int x = startX; x <= startX + maxRadius; x += stepSize) {
            if (world.getBiome(x, y, startZ) != biome) {
                xMax = x - stepSize;
                break;
            }
        }

        int centerX = (xMin + xMax) / 2;

        int zMin = startZ;
        for (int z = startZ; z >= startZ - maxRadius; z -= stepSize) {
            if (world.getBiome(centerX, y, z) != biome) {
                zMin = z + stepSize;
                break;
            }
        }

        int zMax = startZ;
        for (int z = startZ; z <= startZ + maxRadius; z += stepSize) {
            if (world.getBiome(centerX, y, z) != biome) {
                zMax = z - stepSize;
                break;
            }
        }

        int centerZ = (zMin + zMax) / 2;
        int centerY = getSuitableGroundBlockY(world, centerX, y, centerZ, Settings.MAX_LOCATION_HEIGHT_RANGE);

        return new Location(world, centerX, centerY, centerZ);
    }
}
