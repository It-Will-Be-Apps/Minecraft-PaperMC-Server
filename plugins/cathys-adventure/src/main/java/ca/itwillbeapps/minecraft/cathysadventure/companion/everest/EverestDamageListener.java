package ca.itwillbeapps.minecraft.cathysadventure.companion.everest;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import ca.itwillbeapps.minecraft.cathysadventure.Settings;

public class EverestDamageListener implements Listener {
    private final EverestEntity everestEntity;

    public EverestDamageListener(EverestEntity everestEntity) {
        this.everestEntity = everestEntity;
    }

    /**
     * Listens to damage events to prevent Everest from dying.
     *
     * @param event The damage event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!everestEntity.isEverest(event.getEntity())) {
            return;
        }

        double currentHealth = ((LivingEntity) event.getEntity()).getHealth();

        if (event.getFinalDamage() < currentHealth) {
            return;
        }

        event.setDamage(Math.max(0.0, currentHealth - Settings.GP_MINIMUM_HEALTH));
    }
}
