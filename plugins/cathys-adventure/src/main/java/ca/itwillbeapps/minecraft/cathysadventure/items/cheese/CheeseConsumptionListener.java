package ca.itwillbeapps.minecraft.cathysadventure.items.cheese;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class CheeseConsumptionListener implements Listener {
    private final CheeseItem cheeseItem;

    public CheeseConsumptionListener(CheeseItem cheeseItem) {
        this.cheeseItem = cheeseItem;
    }

    /**
     * Listens to item consumption events to clear effects when a player eats
     * cheese.
     *
     * @param event The item consumption event
     */
    @EventHandler
    public void onItemConsumed(PlayerItemConsumeEvent event) {
        if (!cheeseItem.isCheese(event.getItem())) {
            return;
        }

        event.getPlayer().clearActivePotionEffects();
    }
}
