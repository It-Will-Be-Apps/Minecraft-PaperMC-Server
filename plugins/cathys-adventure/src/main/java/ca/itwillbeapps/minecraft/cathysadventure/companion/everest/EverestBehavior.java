
package ca.itwillbeapps.minecraft.cathysadventure.companion.everest;

import java.util.Comparator;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.scheduler.BukkitTask;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.Settings;
import ca.itwillbeapps.minecraft.cathysadventure.items.cheese.CheeseItem;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;

public class EverestBehavior {
    private final CathysAdventure plugin;
    private final PlayerManager playerManager;
    private final CheeseItem cheeseItem;
    private final EverestManager everestManager;
    private final EverestMovement everestMovement;
    private final EverestCombat everestCombat;

    private BukkitTask everestBehaviorTask;

    public EverestBehavior(CathysAdventure plugin, PlayerManager playerManager, CheeseItem cheeseItem,
            EverestManager everestManager, EverestMovement everestMovement, EverestCombat everestCombat) {

        this.plugin = plugin;
        this.playerManager = playerManager;
        this.cheeseItem = cheeseItem;
        this.everestManager = everestManager;
        this.everestMovement = everestMovement;
        this.everestCombat = everestCombat;
    }

    /**
     * Starts Everest's behavior loop.
     */
    public void start() {
        if (everestBehaviorTask != null && !everestBehaviorTask.isCancelled()) {
            return;
        }

        everestBehaviorTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1l, 10l);

        plugin.getLogger().info("Everest behavior loop started");
    }

    /**
     * Processes one behavior tick for Everest.
     */
    private void tick() {
        Wolf everest = everestManager.getEverest();
        Player cathy = playerManager.getCathy();
        
        if (everest == null || everest.isDead() || !everest.isValid() || cathy == null) {
            return;
        }

        if (everestCombat.getEverestIsEnraged()) {
            return;
        }
        
        if (everest.isTamed()) {
            if (everest.getOwner() == null || !everest.getOwner().getUniqueId().equals(cathy.getUniqueId())) {
                everest.setOwner(cathy);
            }
            
            if (everest.isSitting()) {
                return;
            }

            if (!everest.getWorld().getUID().equals(cathy.getWorld().getUID())) {
                everestMovement.teleportNearPlayer(everest, cathy, Settings.GP_SUMMON_DISTANCE, true);
            }

            double squaredDistanceFromCathy = everest.getLocation().distanceSquared(cathy.getLocation());

            if (squaredDistanceFromCathy > Settings.GP_TELEPORT_THRESHOLD * Settings.GP_TELEPORT_THRESHOLD) {
                everestMovement.teleportNearPlayer(everest, cathy, Settings.GP_SUMMON_DISTANCE, true);
            }

            return;
        }

        if (cheeseItem.isCheese(cathy.getInventory().getItemInMainHand())) {
            approachCathy(everest, cathy, true);
            return;
        }

        Player player = findNearestThreateningPlayer(everest);
        if (player != null) {
            everestMovement.moveDistanceFromPlayer(everest, player, Settings.EVEREST_SAFE_DISTANCE, Settings.EVEREST_FLEEING_SPEED);
            return;
        }

        approachCathy(everest, cathy, false);
    }

    /**
     * Makes Everest approach Cathy in the best way based on the distance
     * between them.
     *
     * @param everest The Everest entity
     * @param cathy The Cathy player
     * @param distance The distance from Cathy to aim for
     * @param speed The speed multipler at which Everest should move
     */
    private void approachCathy(Wolf everest, Player cathy, boolean holdingCheese) {
        if (!everest.getWorld().getUID().equals(cathy.getWorld().getUID())) {
            everestMovement.teleportNearPlayer(everest, cathy, Settings.EVEREST_SAFE_DISTANCE, true);
            return;
        }

        double squaredDistanceFromCathy = everest.getLocation().distanceSquared(cathy.getLocation());

        if (squaredDistanceFromCathy > Settings.GP_TELEPORT_THRESHOLD * Settings.GP_TELEPORT_THRESHOLD) {
            everestMovement.teleportNearPlayer(everest, cathy, Settings.EVEREST_SAFE_DISTANCE, false);
        } else if (holdingCheese) {
            everestMovement.moveDistanceFromPlayer(everest, cathy, Settings.GP_SUMMON_DISTANCE, Settings.EVEREST_CHEESE_SPEED);
        } else if (squaredDistanceFromCathy > Settings.EVEREST_APPROACH_THRESHOLD * Settings.EVEREST_APPROACH_THRESHOLD) {
            everestMovement.moveDistanceFromPlayer(everest, cathy, Settings.EVEREST_SAFE_DISTANCE, Settings.EVEREST_STALKING_SPEED);
        }
    }

    /**
     * Finds the nearest player within Everest's threat range, if any.
     *
     * @param everest The Everest entity
     * @return The closest player within the threat range, or null if no player
     *         is in that range
     */
    private Player findNearestThreateningPlayer(Wolf everest) {
        return everest.getWorld().getNearbyPlayers(everest.getLocation(), Settings.EVEREST_THREAT_RADIUS)
            .stream()
            .min(Comparator.comparingDouble(player -> player.getLocation().distanceSquared(everest.getLocation())))
            .orElse(null);
    }

    /**
     * Stops Everest's behavior loop and cancels current movement.
     */
    public void stop() {
        if (everestBehaviorTask != null) {
            everestBehaviorTask.cancel();
            everestBehaviorTask = null;
        }

        plugin.getLogger().info("Everest behavior loop stopped");
    }
}
