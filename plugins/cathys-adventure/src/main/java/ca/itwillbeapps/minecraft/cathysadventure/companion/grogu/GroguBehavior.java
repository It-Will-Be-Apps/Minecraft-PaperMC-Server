
package ca.itwillbeapps.minecraft.cathysadventure.companion.grogu;

import org.bukkit.Location;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.Settings;
import ca.itwillbeapps.minecraft.cathysadventure.companion.CompanionMovement;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;
import ca.itwillbeapps.minecraft.cathysadventure.util.LocationFinder;

public class GroguBehavior {
    private final CathysAdventure plugin;
    private final PlayerManager playerManager;
    private final GroguManager groguManager;
    private final CompanionMovement companionMovement;

    private BukkitTask groguBehaviorTask;
    private int roamingTicks;
    private Location destination;

    public GroguBehavior(CathysAdventure plugin, PlayerManager playerManager, GroguManager groguManager, CompanionMovement companionMovement) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.groguManager = groguManager;
        this.companionMovement = companionMovement;
    }

    /**
     * Starts Grogu's behavior loop.
     */
    public void start() {
        if (groguBehaviorTask != null && !groguBehaviorTask.isCancelled()) {
            return;
        }

        groguBehaviorTask = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1l, 10l);

        roamingTicks = 0;
        destination = null;

        plugin.getLogger().info("Grogu behavior loop started");
    }

    /**
     * Processes one behavior tick for Grogu.
     */
    private void tick() {
        Allay allay = groguManager.getGrogu();

        if (allay != null && !allay.isDead() && allay.isValid()) {
            if (!allay.getPersistentDataContainer().getOrDefault(plugin.getKeys().tamedKey, PersistentDataType.BOOLEAN, false)) {
                untamedGroguTick(allay);
                return;
            }

            Player cathy = playerManager.getCathy();

            if (cathy == null) {
                return;
            }

            tamedGroguTick(allay, cathy);
        }
    }

    /**
     * Processes one behavior tick for untamed Grogu.
     *
     * @param grogu The Grogu entity
     */
    private void untamedGroguTick(Allay grogu) {
        roamingTicks++;

        if (roamingTicks / 2 < Settings.GROGU_MIN_ROAMING_DELAY) {
            if (destination != null) {
                companionMovement.moveToDestination(grogu, destination);
            }

            return;
        }

        roam(grogu);

        double durationUntilNextRoam = Math.random() * (Settings.GROGU_MAX_ROAMING_DELAY - Settings.GROGU_MIN_ROAMING_DELAY) + Settings.GROGU_MIN_ROAMING_DELAY;
        int ticksUntilNextRoam = (int) (2 * (durationUntilNextRoam - Settings.GROGU_MIN_ROAMING_DELAY));
        roamingTicks = -1 * ticksUntilNextRoam;
    }

    /**
     * Makes Grogu roam near his saved jail location.
     *
     * @param grogu The Grogu entity
     */
    private void roam(Allay grogu) {
        int[] groguJailCoordinates = grogu.getPersistentDataContainer().get(plugin.getKeys().groguJailLocationKey, PersistentDataType.INTEGER_ARRAY);
        Location groguJailLocation = new Location(grogu.getWorld(), groguJailCoordinates[0], groguJailCoordinates[1], groguJailCoordinates[2]);

        destination = LocationFinder.getRandomPathfindableLocation(grogu, groguJailLocation, Settings.GROGU_ROAMING_RADIUS, 0);
        companionMovement.moveToDestination(grogu, destination);
    }

    /**
     * Processes one behavior tick for tamed Grogu.
     *
     * @param allay The Grogu entity
     * @param cathy The Cathy player
     */
    private void tamedGroguTick(Allay grogu, Player cathy) {
        if (!grogu.getWorld().getUID().equals(cathy.getWorld().getUID())) {
            companionMovement.teleportNearPlayer(grogu, cathy, Settings.GROGU_SUMMON_DISTANCE, true);
            return;
        }

        double squaredDistanceFromCathy = grogu.getLocation().distanceSquared(cathy.getLocation());

        if (squaredDistanceFromCathy > Settings.GROGU_TELEPORT_THRESHOLD * Settings.GROGU_TELEPORT_THRESHOLD) {
            companionMovement.teleportNearPlayer(grogu, cathy, Settings.GROGU_SUMMON_DISTANCE, false);
        } else if (squaredDistanceFromCathy > Settings.GROGU_SUMMON_DISTANCE * Settings.GROGU_SUMMON_DISTANCE) {
            companionMovement.moveDistanceFromPlayer(grogu, cathy, Settings.GROGU_SUMMON_DISTANCE, Settings.GROGU_SUMMON_DISTANCE);
        }
    }

    /**
     * Stops Grogu's behavior loop.
     */
    public void stop() {
        if (groguBehaviorTask != null) {
            groguBehaviorTask.cancel();
            groguBehaviorTask = null;
        }

        roamingTicks = 0;
        destination = null;

        plugin.getLogger().info("Grogu behavior loop stopped");
    }
}
