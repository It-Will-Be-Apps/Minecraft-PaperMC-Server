package ca.itwillbeapps.minecraft.cathysadventure.items;

import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;

public class ItemDamageListener implements Listener {
    private final CathysAdventure plugin;

    public ItemDamageListener(CathysAdventure plugin) {
        this.plugin = plugin;
    }

    /**
     * Listens to items taking damage to preserve soulbound items.
     */
    @EventHandler
    public void onItemDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Item item && itemIsSoulbound(item.getItemStack())) {
            if (event.getCause() == EntityDamageEvent.DamageCause.CONTACT || event.getCause() == EntityDamageEvent.DamageCause.LAVA
                    || event.getCause() == EntityDamageEvent.DamageCause.FIRE) {

                event.setCancelled(true);
            }
        }
    }

    /**
     * Listens to items taking damage to preserve soulbound items.
     */
    @EventHandler
    public void onItemDamageByBlock(EntityDamageByBlockEvent event) {
        if (event.getEntity() instanceof Item item && itemIsSoulbound(item.getItemStack())) {
            event.setCancelled(true);
        }
    }

    /**
     * Listens to items combusting to preserve soulbound items.
     */
    @EventHandler
    public void onItemCombust(EntityCombustByBlockEvent event) {
        if (event.getEntity() instanceof Item item && itemIsSoulbound(item.getItemStack())) {
            event.setCancelled(true);
        }
    }

    /**
     * Verifies whether the given item is soulbound and should be
     * indestructible.
     *
     * @param item The item to verify
     * @return Whether the item should be indestructible
     */
    private boolean itemIsSoulbound(ItemStack item) {
        return item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(plugin.getKeys().soulboundItemKey);
    }
}
