package ca.itwillbeapps.minecraft.cathysadventure.companion.everest;

import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import io.papermc.paper.event.entity.EntityToggleSitEvent;

public class EverestSittingListener implements Listener {
    private final EverestEntity everestEntity;

    public EverestSittingListener(EverestEntity everestEntity) {
        this.everestEntity = everestEntity;
    }

    /**
     * Listens to sitting events, to prevent Everest from auto-sitting when
     * Cathy changes dimensions.
     *
     * @param event The sitting event
     */
    @EventHandler
    public void onDogSit(EntityToggleSitEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)) {
            return;
        }

        if (!everestEntity.isEverest(wolf)) {
            return;
        }

        if (!isTriggeredByDimensionChange(wolf)) {
            return;
        }

        event.setCancelled(true);
    }
    
    /**
     * Verifies if the wolf's owner is in a different dimension.
     *
     * @param wolf The sitting wolf
     * @return Whether the owner is unavailable or in a different dimension
     */
    private boolean isTriggeredByDimensionChange(Wolf wolf) {
        if (wolf.getOwner() instanceof Player player) {
            return !wolf.getWorld().equals(player.getWorld());
        }

        return true; 
    }
}
