package ca.itwillbeapps.minecraft.cathysadventure.items.cheese;

import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;

public class CheeseCraftingListener implements Listener {
    private final CheeseItem cheeseItem;

    public CheeseCraftingListener(CheeseItem cheeseItem) {
        this.cheeseItem = cheeseItem;
    }

    /**
     * Listens to craft preparation events to prevent from crafting items with
     * cheese.
     * 
     * @param event The craft preparation event
     */
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();

        for (ItemStack item : matrix) {
            if (item != null && !item.getType().isAir()) {
                if (cheeseItem.isCheese(item)) {
                    inventory.setResult(null);
                    break;
                }
            }
        }
    }

    /**
     * Listens to crafter craft events to prevent from crafting items with
     * cheese.
     * 
     * @param event The crafter craft event
     */
    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        if (!(event.getBlock().getState() instanceof Crafter crafter)) {
            return;
        }

        for (ItemStack item : crafter.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                if (cheeseItem.isCheese(item)) {
                    event.setResult(ItemStack.empty());
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }
}
