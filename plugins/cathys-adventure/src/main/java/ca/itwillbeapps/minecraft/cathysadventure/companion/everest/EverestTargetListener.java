package ca.itwillbeapps.minecraft.cathysadventure.companion.everest;

import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;

public class EverestTargetListener implements Listener {
    private final EverestEntity everestEntity;

    public EverestTargetListener(EverestEntity everestEntity) {
        this.everestEntity = everestEntity;
    }

    /**
     * Listens to target events to stop Everest from targetting animals.
     *
     * @param event The target event
     */
    @EventHandler
    public void onTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)) {
            return;
        }

        if (!everestEntity.isEverest(wolf)) {
            return;
        }

        if (wolf.isTamed()) {
            return;
        }

        event.setCancelled(true);
    }
}
