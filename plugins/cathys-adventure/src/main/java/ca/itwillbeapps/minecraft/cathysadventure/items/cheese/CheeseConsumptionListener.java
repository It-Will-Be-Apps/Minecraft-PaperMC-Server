package ca.itwillbeapps.minecraft.cathysadventure.items.cheese;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public final class CheeseConsumptionListener implements Listener {
    private final CheeseItem cheeseItem;

    public CheeseConsumptionListener(CheeseItem cheeseItem) {
        this.cheeseItem = cheeseItem;
    }

    @EventHandler
    public void onItemConsumed(PlayerItemConsumeEvent event) {
        if (!cheeseItem.isCheese(event.getItem())) {
            return;
        }

        event.getPlayer().clearActivePotionEffects();
    }
}
