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
import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.GreatPyreneesManager;
import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.GreatPyreneesMovement;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;

public class SummoningHornInteractionListener implements Listener {
    private final PlayerManager playerManager;
    private final GreatPyreneesManager greatPyreneesManager;
    private final GreatPyreneesMovement greatPyreneesMovement;
    private final SummoningHornItem summoningHornItem;

    public SummoningHornInteractionListener(PlayerManager playerManager, GreatPyreneesManager greatPyreneesManager, GreatPyreneesMovement greatPyreneesMovement,
            SummoningHornItem summoningHornItem) {

        this.playerManager = playerManager;
        this.greatPyreneesManager = greatPyreneesManager;
        this.greatPyreneesMovement = greatPyreneesMovement;
        this.summoningHornItem = summoningHornItem;
    }

    /**
     * Listens to item interaction events to summon the great pyrenees.
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

        for (Wolf wolf : greatPyreneesManager.getGreatPyrenees()) {
            if (!wolf.isDead() && wolf.isValid() && wolf.isTamed()) {
                wolf.setSitting(false);
                greatPyreneesMovement.teleportNearPlayer(wolf, cathy, Settings.GP_SUMMON_DISTANCE, true);
            }
        }
    }
}
