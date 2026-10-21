
package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.util.Vector;

import ca.itwillbeapps.minecraft.cathysadventure.util.LocationFinder;

public class GreatPyreneesMovement {

    /**
     * Has a great pyrenees move to the given distance from the given player.
     *
     * @param wolf The great pyrenees entity
     * @param player The player to approach
     * @param distance The target distance from the player
     * @param speed The speed multipler at which the great pyrenees should move
     */
    public void moveDistanceFromPlayer(Wolf wolf, Player player, double distance, double speed) {
        Location destination = getMovementDestination(wolf, player, distance);
        wolf.getPathfinder().moveTo(destination, speed);
    }

    /**
     * Calculates a location for a great pyrenees to go to near the given
     * location.
     *
     * @param wolf The great pyrenees entity
     * @param player The player to move a certain distance from
     * @param distance The target distance from the player
     * @return The location to go to
     */
    private Location getMovementDestination(Wolf wolf, Player player, double distance) {
        Location greatPyreneeLocation = wolf.getLocation();
        Location playerLocation = player.getLocation();

        if (!wolf.getWorld().getUID().equals(player.getWorld().getUID())) {
            return playerLocation.clone().add(new Vector(distance, 0.0, 0.0));
        }

        Vector directionVector = greatPyreneeLocation.toVector().subtract(playerLocation.toVector()).setY(0.0);

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
     * Has a great pyrenees move to the given destination.
     *
     * @param wolf The great pyrenees entity
     * @param destination The destination to move to
     */
    public void moveToDestination(Wolf wolf, Location destination) {
        wolf.getPathfinder().moveTo(destination);
    }

    /**
     * Teleports a great pyrenees to a safe spot near the given player.
     *
     * @param wolf The great pyrenees entity
     * @param player The player near which to teleport the great pyrenees
     * @param distance The target distance from the player
     * @param force Whether to force a teleport even if no safe spot can be
     *        found
     */
    public void teleportNearPlayer(Wolf wolf, Player player, int distance, boolean force) {
        Location destination = getMovementDestination(wolf, player, distance);
        Location safeDestination = LocationFinder.nearLocation(destination);

        if (safeDestination == null) {
            if (force) {
                wolf.getPathfinder().stopPathfinding();
                wolf.teleport(destination);
            }

            return;
        }

        wolf.getPathfinder().stopPathfinding();
        wolf.teleport(safeDestination);
    }

    /**
     * Stops a great pyrenees' current path finding.
     *
     * @param wolf The great pyrenees entity
     */
    public void stopPathFinding(Wolf wolf) {
        if (wolf == null || wolf.isDead() || !wolf.isValid()) {
            return;
        }

        wolf.getPathfinder().stopPathfinding();
    }
}
