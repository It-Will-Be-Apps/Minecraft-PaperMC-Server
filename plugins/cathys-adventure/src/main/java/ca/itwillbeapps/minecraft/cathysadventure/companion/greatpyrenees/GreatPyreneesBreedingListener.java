package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import org.bukkit.Bukkit;
import org.bukkit.entity.Animals;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityEnterLoveModeEvent;

import ca.itwillbeapps.minecraft.cathysadventure.Settings;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;

public class GreatPyreneesBreedingListener implements Listener {
    private final PlayerManager playerManager;
    private final GreatPyreneesManager greatPyreneesManager;
    private final EverestEntity everestEntity;
    private final ObelixEntity obelixEntity;

    public GreatPyreneesBreedingListener(PlayerManager playerManager, GreatPyreneesManager greatPyreneesManager, EverestEntity everestEntity,
            ObelixEntity obelixEntity) {

        this.playerManager = playerManager;
        this.greatPyreneesManager = greatPyreneesManager;
        this.everestEntity = everestEntity;
        this.obelixEntity = obelixEntity;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEnterLoveMode(EntityEnterLoveModeEvent event) {
        Animals animal = event.getEntity();

        if (!(animal instanceof Wolf wolf)) {
            return;
        }

        boolean isEverest = everestEntity.isEverest(wolf);
        boolean isObelix = obelixEntity.isObelix(wolf);

        if (!isEverest && !isObelix) {
            return;
        }

        Wolf partner = findPartner(wolf);

        if (partner == null) {
            event.setCancelled(true);
        }
    }

    private Wolf findPartner(Wolf wolf) {
        return wolf.getWorld().getNearbyEntities(wolf.getLocation(), Settings.GP_BREEDING_DISTANCE, Settings.GP_BREEDING_DISTANCE,
                Settings.GP_BREEDING_DISTANCE, entity -> {

            if (!(entity instanceof Wolf candidate)) {
                return false;
            }

            if (!everestEntity.isEverest(candidate) && !obelixEntity.isObelix(candidate)) {
                return false;
            }

            return candidate.isTamed();
        }).stream().map(Wolf.class::cast).findFirst().orElse(null);
    }

    /**
     * Listens to breeding events to handle great pyrenees breeding.
     *
     * @param event The breeding event
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreed(EntityBreedEvent event) {
        if (event.getEntityType() != EntityType.WOLF) {
            return;
        }

        Wolf father = (Wolf) event.getFather();
        Wolf mother = (Wolf) event.getMother();

        if (everestEntity.isEverest(father) || everestEntity.isEverest(mother)
                || obelixEntity.isObelix(father) || obelixEntity.isObelix(mother)) {

            Player cathy = playerManager.getCathy();
    
            if (cathy == null || event.getBreeder() != cathy) {
                return;
            }

            if ((everestEntity.isEverest(father) || everestEntity.isEverest(mother))
                    && (obelixEntity.isObelix(father) || obelixEntity.isObelix(mother))) {

                if (!greatPyreneesManager.canPuppySpawn()) {
                    event.setCancelled(true);
                    return;
                }

                Wolf puppy = (Wolf) event.getEntity();
                greatPyreneesManager.registerPuppy(puppy);
            }
        }
    }
}
