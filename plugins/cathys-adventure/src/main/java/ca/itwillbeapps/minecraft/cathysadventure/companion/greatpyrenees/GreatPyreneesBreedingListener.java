package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;

import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.everest.EverestEntity;
import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.obelix.ObelixEntity;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;

public class GreatPyreneesBreedingListener implements Listener {
    private final PlayerManager playerManager;
    private final EverestEntity everestEntity;
    private final ObelixEntity obelixEntity;

    public GreatPyreneesBreedingListener(PlayerManager playerManager, EverestEntity everestEntity, ObelixEntity obelixEntity) {
        this.playerManager = playerManager;
        this.everestEntity = everestEntity;
        this.obelixEntity = obelixEntity;
    }

    /**
     * Listens to breeding events to prevent Everest from breeding.
     *
     * @param event The breeding event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) { // TODO update javadoc, make this work for all GPs
        if (event.getEntityType() != EntityType.WOLF) {
            return;
        }

        Wolf father = (Wolf) event.getFather();
        Wolf mother = (Wolf) event.getMother();

        if (everestEntity.isEverest(father) || everestEntity.isEverest(mother)) {
            Player cathy = playerManager.getCathy();
    
            if (cathy == null || event.getBreeder() != cathy) {
                event.setCancelled(true);
                return;
            }

            event.setCancelled(true); // TODO eventually here let her breed with Obélix
            return;
        }

    }
}
