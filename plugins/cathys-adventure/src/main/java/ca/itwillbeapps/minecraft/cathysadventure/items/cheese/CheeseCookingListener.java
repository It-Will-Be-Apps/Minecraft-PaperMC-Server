package ca.itwillbeapps.minecraft.cathysadventure.items.cheese;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.inventory.ItemStack;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;

public class CheeseCookingListener implements Listener {
    private final CathysAdventure plugin;
    private final CheeseItem cheeseItem;

    public CheeseCookingListener(CathysAdventure plugin, CheeseItem cheeseItem) {
        this.plugin = plugin;
        this.cheeseItem = cheeseItem;
    }

    /**
     * Listens to cooking blocks to refund a bucket when a bucket of milk is
     * cooked.
     *
     * @param event The block cooking event
     */
    @EventHandler
    public void onBlockCook(BlockCookEvent event) {
        if (!cheeseItem.isCheese(event.getResult())) {
            return;
        }

        Block block = event.getBlock();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!block.getChunk().isLoaded()) {
                return;
            }

            block.getWorld().dropItemNaturally(block.getLocation().add(0.0, 1.0, 0.0), new ItemStack(Material.BUCKET));
        });
    }
}
