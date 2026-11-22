package ca.itwillbeapps.minecraft.cathysadventure.companion.grogu;

import org.bukkit.Bukkit;
import org.bukkit.GameEvent;
import org.bukkit.entity.Allay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.world.GenericGameEvent;

public class GroguSoundListener implements Listener {
    private final GroguEntity groguEntity;

    public GroguSoundListener(GroguEntity groguEntity) {
        this.groguEntity = groguEntity;
    }

    /**
     * Listens to vibrations for Grogu to disable custom allay behaviors.
     *
     * @param event The vibration event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVibration(GenericGameEvent event) {
        if (!(event.getEntity() instanceof Allay allay)) {
            return;
        }

        if (!groguEntity.isGrogu(allay)) {
            return;
        }

        if (event.getEvent() == GameEvent.NOTE_BLOCK_PLAY || event.getEvent() == GameEvent.JUKEBOX_PLAY) {
            Bukkit.getLogger().info("Cancelling note event");
            event.setCancelled(true);
        }
    }

    /**
     * Listens to duplication events to Grogu from duplicating.
     *
     * @param event The duplication event
     */
    @EventHandler
    public void onDuplicate(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Allay allay)) {
            return;
        }

        if (!groguEntity.isGrogu(allay)) {
            return;
        }

        if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.DUPLICATION) {
            event.setCancelled(true);
        }
    }
}
