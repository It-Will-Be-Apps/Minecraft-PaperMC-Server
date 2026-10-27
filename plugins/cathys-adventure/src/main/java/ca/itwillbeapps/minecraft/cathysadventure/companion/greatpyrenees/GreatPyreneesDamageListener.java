package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import ca.itwillbeapps.minecraft.cathysadventure.Settings;
import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.everest.EverestEntity;
import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.obelix.ObelixEntity;

public class GreatPyreneesDamageListener implements Listener {
    private final EverestEntity everestEntity;
    private final ObelixEntity obelixEntity;

    public GreatPyreneesDamageListener(EverestEntity everestEntity, ObelixEntity obelixEntity) {
        this.everestEntity = everestEntity;
        this.obelixEntity = obelixEntity;
    }

    /**
     * Listens to damage events to prevent great pyrenees from dying.
     *
     * @param event The damage event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        if (!everestEntity.isEverest(entity) && !obelixEntity.isObelix(entity)) { // TODO Also check for puppies
            return;
        }

        double currentHealth = ((LivingEntity) entity).getHealth();

        if (event.getFinalDamage() < currentHealth) {
            return;
        }

        event.setDamage(Math.max(0.0, currentHealth - Settings.COMPANIONS_MINIMUM_HEALTH));
    }
}
