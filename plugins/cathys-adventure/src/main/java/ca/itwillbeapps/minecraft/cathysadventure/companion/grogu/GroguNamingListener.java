package ca.itwillbeapps.minecraft.cathysadventure.companion.grogu;

import org.bukkit.Material;
import org.bukkit.entity.Allay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public class GroguNamingListener implements Listener {
    private final GroguEntity groguEntity;

    public GroguNamingListener(GroguEntity groguEntity) {
        this.groguEntity = groguEntity;
    }

    /**
     * Listens to name tag interaction events to prevent Grogu from getting
     * renamed.
     * 
     * @param event The interaction event
     */
    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (!(event.getRightClicked() instanceof Allay allay)) {
            return;
        }

        if (!groguEntity.isGrogu(allay)) {
            return;
        }

        Player player = event.getPlayer();

        if (player.getInventory().getItemInMainHand().getType() != Material.NAME_TAG) {
            return;
        }

        event.setCancelled(true);
        player.sendMessage("Grogu cannot be renamed!");
    }
}
