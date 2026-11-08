package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import ca.itwillbeapps.minecraft.cathysadventure.Settings;

public class GreatPyreneesDamageListener implements Listener {
    private final EverestEntity everestEntity;
    private final ObelixEntity obelixEntity;
    private final GreatPyreneesPuppyEntity greatPyreneesPuppyEntity;

    public GreatPyreneesDamageListener(EverestEntity everestEntity, ObelixEntity obelixEntity, GreatPyreneesPuppyEntity greatPyreneesPuppyEntity) {
        this.everestEntity = everestEntity;
        this.obelixEntity = obelixEntity;
        this.greatPyreneesPuppyEntity = greatPyreneesPuppyEntity;
    }

    /**
     * Listens to damage events to prevent great pyrenees from dying.
     *
     * @param event The damage event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();

        if (!everestEntity.isEverest(entity) && !obelixEntity.isObelix(entity) && !greatPyreneesPuppyEntity.isGreatPyreneesPuppy(entity)) {
            return;
        }

        double currentHealth = ((LivingEntity) entity).getHealth();

        if (event.getFinalDamage() < currentHealth) {
            return;
        }

        event.setDamage(Math.max(0.0, currentHealth - Settings.COMPANIONS_MINIMUM_HEALTH));
    }
}
