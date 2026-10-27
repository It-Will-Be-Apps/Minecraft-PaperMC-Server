package ca.itwillbeapps.minecraft.cathysadventure.companion.grogu;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import ca.itwillbeapps.minecraft.cathysadventure.Settings;

public class GroguDamageListener implements Listener {
    private final GroguEntity groguEntity;

    public GroguDamageListener(GroguEntity groguEntity) {
        this.groguEntity = groguEntity;
    }

    /**
     * Listens to damage events to prevent Grogu from dying.
     *
     * @param event The damage event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        if (!groguEntity.isGrogu(entity)) {
            return;
        }

        double currentHealth = ((LivingEntity) entity).getHealth();

        if (event.getFinalDamage() < currentHealth) {
            return;
        }

        event.setDamage(Math.max(0.0, currentHealth - Settings.COMPANIONS_MINIMUM_HEALTH));
    }
}
