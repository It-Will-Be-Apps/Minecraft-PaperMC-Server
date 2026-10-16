package ca.itwillbeapps.minecraft.cathysadventure.items.summoninghorn;

import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import ca.itwillbeapps.minecraft.cathysadventure.Settings;
import ca.itwillbeapps.minecraft.cathysadventure.companion.everest.EverestManager;
import ca.itwillbeapps.minecraft.cathysadventure.companion.everest.EverestMovement;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;

public class SummoningHornInteractionListener implements Listener {
    private final PlayerManager playerManager;
    private final EverestManager everestManager;
    private final EverestMovement everestMovement;
    private final SummoningHornItem summoningHornItem;

    public SummoningHornInteractionListener(PlayerManager playerManager, EverestManager everestManager, EverestMovement everestMovement, SummoningHornItem summoningHornItem) {
        this.playerManager = playerManager;
        this.everestManager = everestManager;
        this.everestMovement = everestMovement;
        this.summoningHornItem = summoningHornItem;
    }

    /**
     * Listens to item interaction events to summon Everest.
     *
     * @param event The interaction event
     */
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getItem();
        if (!summoningHornItem.isSummoningHorn(item)) {
            return;
        }

        Player cathy = playerManager.getCathy();
        if (cathy == null || !event.getPlayer().getUniqueId().equals(cathy.getUniqueId())) {
            return;
        }

        if (cathy.hasCooldown(item)) {
            return;
        }

        Wolf everest = everestManager.getEverest();
        if (everest == null || everest.isDead() || !everest.isValid() || !everest.isTamed()) {
            return;
        }

        everest.setSitting(false);
        everestMovement.teleportNearPlayer(everest, cathy, Settings.GP_SUMMON_DISTANCE, true);
    }
}
