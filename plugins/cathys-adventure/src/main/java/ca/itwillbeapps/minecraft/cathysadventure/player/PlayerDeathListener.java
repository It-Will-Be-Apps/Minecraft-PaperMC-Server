package ca.itwillbeapps.minecraft.cathysadventure.player;

import java.util.List;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;

public class PlayerDeathListener implements Listener {
    private final CathysAdventure plugin;

    public PlayerDeathListener(CathysAdventure plugin) {
        this.plugin = plugin;
    }

    /**
     * Listens to players deaths to keep specials items in their inventory.
     *
     * @param event The player death event
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        List<ItemStack> drops = event.getDrops();
        List<ItemStack> itemsToKeep = event.getDrops().stream().filter(this::itemIsSoulbound).toList();

        drops.removeAll(itemsToKeep);
        event.getItemsToKeep().addAll(itemsToKeep);
    }

    /**
     * Verifies whether the given item is soulbound and should remain on death.
     *
     * @param item The item to verify
     * @return Whether the item should remain on death
     */
    private boolean itemIsSoulbound(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(plugin.getKeys().soulboundItemKey);
    }
}
