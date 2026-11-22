package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public class GreatPyreneesTargetListener implements Listener {
    private final EverestEntity everestEntity;
    private final ObelixEntity obelixEntity;
    private final GreatPyreneesPuppyEntity greatPyreneesPuppyEntity;

    public GreatPyreneesTargetListener(EverestEntity everestEntity, ObelixEntity obelixEntity, GreatPyreneesPuppyEntity greatPyreneesPuppyEntity) {
        this.everestEntity = everestEntity;
        this.obelixEntity = obelixEntity;
        this.greatPyreneesPuppyEntity = greatPyreneesPuppyEntity;
    }

    /**
     * Listens to target events to stop great pyrenees from targetting animals
     * and players.
     *
     * @param event The target event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)) {
            return;
        }

        if (!everestEntity.isEverest(wolf) && !obelixEntity.isObelix(wolf) && !greatPyreneesPuppyEntity.isGreatPyreneesPuppy(wolf)) {
            return;
        }

        if ((event.getTarget() instanceof Player)) {
            event.setCancelled(true);
            return;
        }

        if (wolf.isTamed()) {
            return;
        }

        if (everestEntity.isEverest(wolf)) {
            event.setCancelled(true);
        }
    }
}
