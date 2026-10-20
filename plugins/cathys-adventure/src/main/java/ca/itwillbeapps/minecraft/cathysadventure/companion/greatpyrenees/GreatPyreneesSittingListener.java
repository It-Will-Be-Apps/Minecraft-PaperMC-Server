package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.everest.EverestEntity;
import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.obelix.ObelixEntity;
import io.papermc.paper.event.entity.EntityToggleSitEvent;

public class GreatPyreneesSittingListener implements Listener {
    private final EverestEntity everestEntity;
    private final ObelixEntity obelixEntity;

    public GreatPyreneesSittingListener(EverestEntity everestEntity, ObelixEntity obelixEntity) {
        this.everestEntity = everestEntity;
        this.obelixEntity = obelixEntity;
    }

    /**
     * Listens to sitting events, to prevent great pyrenees from auto-sitting
     * when Cathy changes dimensions.
     *
     * @param event The sitting event
     */
    @EventHandler
    public void onDogSit(EntityToggleSitEvent event) {
        if (!(event.getEntity() instanceof Wolf wolf)) {
            return;
        }

        if (!everestEntity.isEverest(wolf) && !obelixEntity.isObelix(wolf)) { // TODO puppies
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
