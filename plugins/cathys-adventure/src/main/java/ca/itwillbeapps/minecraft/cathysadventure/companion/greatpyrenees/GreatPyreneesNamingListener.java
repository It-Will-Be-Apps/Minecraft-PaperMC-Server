package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;

public class GreatPyreneesNamingListener implements Listener {
    private final PlayerManager playerManager;
    private final EverestEntity everestEntity;
    private final ObelixEntity obelixEntity;
    private final GreatPyreneesPuppyEntity greatPyreneesPuppyEntity;

    public GreatPyreneesNamingListener(PlayerManager playerManager, EverestEntity everestEntity, ObelixEntity obelixEntity, GreatPyreneesPuppyEntity greatPyreneesPuppyEntity) {
        this.playerManager = playerManager;
        this.everestEntity = everestEntity;
        this.obelixEntity = obelixEntity;
        this.greatPyreneesPuppyEntity = greatPyreneesPuppyEntity;
    }

    /**
     * Listens to name tag interaction events to prevent Everest or Obélix from
     * getting renamed.
     * 
     * @param event The interaction event
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!(event.getRightClicked() instanceof Wolf wolf)) {
            return;
        }

        Player player = event.getPlayer();

        if (player.getInventory().getItemInMainHand().getType() != Material.NAME_TAG) {
            return;
        }

        if (!everestEntity.isEverest(wolf) && !obelixEntity.isObelix(wolf) && !greatPyreneesPuppyEntity.isGreatPyreneesPuppy(wolf)) {
            return;
        }

        Player cathy = playerManager.getCathy();

        if (cathy == null || !player.getUniqueId().equals(cathy.getUniqueId())) {
            event.setCancelled(true);
            player.sendMessage("You cannot rename this dog!");
            return;
        }

        if (greatPyreneesPuppyEntity.isGreatPyreneesPuppy(wolf)) {
            return;
        }

        event.setCancelled(true);

        if (everestEntity.isEverest(wolf)) {
            player.sendMessage("Everest cannot be renamed!");
        } else if (obelixEntity.isObelix(wolf)) {
            player.sendMessage("Obélix cannot be renamed!");
        }
    }
}
