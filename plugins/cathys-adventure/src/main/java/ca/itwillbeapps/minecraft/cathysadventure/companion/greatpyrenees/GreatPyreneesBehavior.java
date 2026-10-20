
package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import java.util.Comparator;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.scheduler.BukkitTask;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.Settings;
import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.everest.EverestEntity;
import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.obelix.ObelixEntity;
import ca.itwillbeapps.minecraft.cathysadventure.items.cheese.CheeseItem;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;

public class GreatPyreneesBehavior {
    private final CathysAdventure plugin;
    private final PlayerManager playerManager;
    private final CheeseItem cheeseItem;
    private final EverestEntity everestEntity;
    private final ObelixEntity obelixEntity;
    private final GreatPyreneesManager greatPyreneesManager;
    private final GreatPyreneesMovement greatPyreneesMovement;
    private final GreatPyreneesCombat greatPyreneesCombat;

    private BukkitTask greatPyreneesBehaviorTask;

    public GreatPyreneesBehavior(CathysAdventure plugin, PlayerManager playerManager, CheeseItem cheeseItem, EverestEntity everestEntity, ObelixEntity obelixEntity,
            GreatPyreneesManager greatPyreneesManager, GreatPyreneesMovement greatPyreneesMovement, GreatPyreneesCombat greatPyreneesCombat) {

        this.plugin = plugin;
        this.playerManager = playerManager;
        this.cheeseItem = cheeseItem;
        this.everestEntity = everestEntity;
        this.obelixEntity = obelixEntity;
        this.greatPyreneesManager = greatPyreneesManager;
        this.greatPyreneesMovement = greatPyreneesMovement;
        this.greatPyreneesCombat = greatPyreneesCombat;
    }

    /**
     * Starts the great pyrenees' behavior loop.
     */
    public void start() {
        if (greatPyreneesBehaviorTask != null && !greatPyreneesBehaviorTask.isCancelled()) {
            return;
        }

        greatPyreneesBehaviorTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1l, 10l);

        plugin.getLogger().info("Great Pyrenees behavior loop started");
    }

    /**
     * Processes one behavior tick for the great pyrenees.
     */
    private void tick() {
        Player cathy = playerManager.getCathy();

        if (cathy == null) {
            return;
        }

        if (greatPyreneesCombat.getGreatPyreneesAreEnraged()) {
            return;
        }

        for (Wolf wolf : greatPyreneesManager.getGreatPyrenees()) {
            if (!wolf.isDead() && wolf.isValid()) {
                if (wolf.isTamed()) {
                    tamedGreatPyreneesTick(wolf, cathy);
                    break;
                }

                if (everestEntity.isEverest(wolf)) {
                    untamedEverestTick(wolf, cathy);
                } else if (obelixEntity.isObelix(wolf)) {
                    untamedObelixTick(wolf);
                }
            }
        }
    }

    /**
     * Processes one behavior tick for the given untamed great pyrenees.
     *
     * @param wolf The great pyrenees entity
     * @param cathy The Cathy player
     */
    private void tamedGreatPyreneesTick(Wolf wolf, Player cathy) {
        if (wolf.getOwner() == null || !wolf.getOwner().getUniqueId().equals(cathy.getUniqueId())) {
            wolf.setOwner(cathy);
        }
        
        if (wolf.isSitting()) {
            return;
        }

        if (!wolf.getWorld().getUID().equals(cathy.getWorld().getUID())) {
            greatPyreneesMovement.teleportNearPlayer(wolf, cathy, Settings.GP_SUMMON_DISTANCE, true);
        }

        double squaredDistanceFromCathy = wolf.getLocation().distanceSquared(cathy.getLocation());

        if (squaredDistanceFromCathy > Settings.GP_TELEPORT_THRESHOLD * Settings.GP_TELEPORT_THRESHOLD) {
            greatPyreneesMovement.teleportNearPlayer(wolf, cathy, Settings.GP_SUMMON_DISTANCE, true);
        }
    }

    /**
     * Processes one behavior tick for untamed Everest.
     *
     * @param everest The Everest entity
     * @param cathy The Cathy player
     */
    private void untamedEverestTick(Wolf everest, Player cathy) {
        if (cheeseItem.isCheese(cathy.getInventory().getItemInMainHand())) {
            approachCathy(everest, cathy, true);
            return;
        }

        Player player = findNearestThreateningPlayer(everest);

        if (player != null) {
            greatPyreneesMovement.moveDistanceFromPlayer(everest, player, Settings.EVEREST_SAFE_DISTANCE, Settings.EVEREST_FLEEING_SPEED);
            return;
        }

        approachCathy(everest, cathy, false);
    }

    /**
     * Processes one behavior tick for untamed Obelix.
     *
     * @param obelix The Obelix entity
     */
    private void untamedObelixTick(Wolf obelix) {
        // TODO stay within his biome
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
            greatPyreneesMovement.teleportNearPlayer(everest, cathy, Settings.EVEREST_SAFE_DISTANCE, true);
            return;
        }

        double squaredDistanceFromCathy = everest.getLocation().distanceSquared(cathy.getLocation());

        if (squaredDistanceFromCathy > Settings.GP_TELEPORT_THRESHOLD * Settings.GP_TELEPORT_THRESHOLD) {
            greatPyreneesMovement.teleportNearPlayer(everest, cathy, Settings.EVEREST_SAFE_DISTANCE, false);
        } else if (holdingCheese) {
            greatPyreneesMovement.moveDistanceFromPlayer(everest, cathy, Settings.GP_SUMMON_DISTANCE, Settings.EVEREST_CHEESE_SPEED);
        } else if (squaredDistanceFromCathy > Settings.EVEREST_APPROACH_THRESHOLD * Settings.EVEREST_APPROACH_THRESHOLD) {
            greatPyreneesMovement.moveDistanceFromPlayer(everest, cathy, Settings.EVEREST_SAFE_DISTANCE, Settings.EVEREST_STALKING_SPEED);
        }
    }

    /**
     * Stops the great pyrenees' behavior loop and cancels current movement.
     */
    public void stop() {
        if (greatPyreneesBehaviorTask != null) {
            greatPyreneesBehaviorTask.cancel();
            greatPyreneesBehaviorTask = null;
        }

        plugin.getLogger().info("Great Pyrenees behavior loop stopped");
    }
}
