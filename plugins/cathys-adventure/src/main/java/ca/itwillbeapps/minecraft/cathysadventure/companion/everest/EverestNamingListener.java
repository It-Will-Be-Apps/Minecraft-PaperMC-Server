package ca.itwillbeapps.minecraft.cathysadventure.companion.everest;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class EverestNamingListener implements Listener {
    private final EverestEntity everestEntity;

    public EverestNamingListener(EverestEntity everestEntity) {
        this.everestEntity = everestEntity;
    }

    /**
     * Listens to name tage interaction events to prevent Everest from getting
     * renamed.
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

        if (!everestEntity.isEverest(wolf)) {
            return;
        }

        Player player = event.getPlayer();

        if (player.getInventory().getItemInMainHand().getType() != Material.NAME_TAG) {
            return;
        }

        event.setCancelled(true);    
        player.sendMessage("Everest cannot be renamed!");
    }
}
