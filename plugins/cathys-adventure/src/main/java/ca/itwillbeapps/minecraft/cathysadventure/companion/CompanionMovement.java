
package ca.itwillbeapps.minecraft.cathysadventure.companion;

import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import ca.itwillbeapps.minecraft.cathysadventure.Settings;
import ca.itwillbeapps.minecraft.cathysadventure.util.LocationFinder;

public class CompanionMovement {

    /**
     * Has a companion move to the given distance from the given player.
     *
     * @param mob The companion entity
     * @param player The player to approach
     * @param distance The target distance from the player
     * @param speed The speed multipler at which the companion should move
     */
    public void moveDistanceFromPlayer(Mob mob, Player player, double distance, double speed) {
        Location destination = getMovementDestination(mob, player, distance);
        mob.getPathfinder().moveTo(destination, speed);
    }

    /**
     * Calculates a location for a companion to go to near the given location.
     *
     * @param mob The companion entity
     * @param player The player to move a certain distance from
     * @param distance The target distance from the player
     * @return The location to go to
     */
    private Location getMovementDestination(Mob mob, Player player, double distance) {
        Location companionLocation = mob.getLocation();
        Location playerLocation = player.getLocation();

        if (!mob.getWorld().getUID().equals(player.getWorld().getUID())) {
            return playerLocation.clone().add(new Vector(distance, 0.0, 0.0));
        }

        Vector directionVector = companionLocation.toVector().subtract(playerLocation.toVector()).setY(0.0);

        if (directionVector.lengthSquared() < 0.001) {
            directionVector = new Vector(1.0, 0.0, 0.0);
        }

        directionVector.normalize().multiply(distance);
        addRandomOffsetToLocation(directionVector);

        return playerLocation.clone().add(directionVector);
    }

    /**
     * Adds a small random offset to the given vector.
     *
     * @param vector The vector to offset
     */
    private void addRandomOffsetToLocation(Vector vector) {
        vector.setX(vector.getX() + (Math.random() * 2) - 1);
        vector.setZ(vector.getZ() + (Math.random() * 2) - 1);
    }

    /**
     * Has a companion move to the given destination.
     *
     * @param mob The companion entity
     * @param destination The destination to move to
     */
    public void moveToDestination(Mob mob, Location destination) {
        mob.getPathfinder().moveTo(destination);
    }

    /**
     * Teleports a companion to a safe spot near the given player.
     *
     * @param mob The companion entity
     * @param player The player near which to teleport the companion
     * @param distance The target distance from the player
     * @param force Whether to force a teleport even if no safe spot can be
     *        found
     */
    public void teleportNearPlayer(Mob mob, Player player, int distance, boolean force) {
        Location destination = getMovementDestination(mob, player, distance);
        Location safeDestination = LocationFinder.nearLocation(destination, 0, Settings.LOCATION_HEIGHT_RANGE);

        if (safeDestination == null) {
            if (force) {
                mob.getPathfinder().stopPathfinding();
                mob.teleport(destination);
            }

            return;
        }

        mob.getPathfinder().stopPathfinding();
        mob.teleport(safeDestination);
    }

    /**
     * Stops a companion' current path finding.
     *
     * @param mob The companion entity
     */
    public void stopPathFinding(Mob mob) {
        if (mob == null || mob.isDead() || !mob.isValid()) {
            return;
        }

        mob.getPathfinder().stopPathfinding();
    }
}
