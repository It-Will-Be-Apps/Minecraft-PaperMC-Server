
package ca.itwillbeapps.minecraft.cathysadventure.companion.everest;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.util.Vector;

import ca.itwillbeapps.minecraft.cathysadventure.companion.LocationFinder;

public class EverestMovement {

    /**
     * Has Everest move to the given distance from the given player.
     *
     * @param everest The Everest entity
     * @param player The player to approach
     * @param distance The target distance from the player
     * @param speed The speed multipler at which Everest should move
     */
    public void moveDistanceFromPlayer(Wolf everest, Player player, double distance, double speed) {
        Location destination = getMovementDestination(everest, player, distance);
        everest.getPathfinder().moveTo(destination, speed);
    }

    /**
     * Calculates a location for Everest to go to near the given location.
     *
     * @param everest The Everest entity
     * @param player The player to move a certain distance from
     * @param distance The target distance from the player
     * @return The location to go to
     */
    private Location getMovementDestination(Wolf everest, Player player, double distance) {
        Location everestLocation = everest.getLocation();
        Location playerLocation = player.getLocation();

        if (!everest.getWorld().getUID().equals(player.getWorld().getUID())) {
            return playerLocation.clone().add(new Vector(distance, 0.0, 0.0));
        }

        Vector directionVector = everestLocation.toVector().subtract(playerLocation.toVector()).setY(0.0);

        if (directionVector.lengthSquared() < 0.001) {
            directionVector = new Vector(1.0, 0.0, 0.0);
        }

        directionVector.normalize().multiply(distance);

        return playerLocation.clone().add(directionVector);
    }

    /**
     * Teleports Everest to a safe spot near the given player.
     *
     * @param everest The Everest entity
     * @param player The player near which to teleport Everest
     * @param distance The target distance from the player
     * @param force Whether to force a teleport even if no safe spot can be
     *        found
     */
    public void teleportNearPlayer(Wolf everest, Player player, int distance, boolean force) {
        Location destination = getMovementDestination(everest, player, distance);
        Location safeDestination = LocationFinder.nearLocation(player.getWorld(), destination);

        if (safeDestination == null) {
            if (force) {
                everest.getPathfinder().stopPathfinding();
                everest.teleport(destination);
            }

            return;
        }

        everest.getPathfinder().stopPathfinding();
        everest.teleport(safeDestination);
    }

    /**
     * Stops Everest's current path finding.
     *
     * @param everest The Everest entity
     */
    public void stopPathFinding(Wolf everest) {
        if (everest == null || everest.isDead() || !everest.isValid()) {
            return;
        }

        everest.getPathfinder().stopPathfinding();
    }
}
