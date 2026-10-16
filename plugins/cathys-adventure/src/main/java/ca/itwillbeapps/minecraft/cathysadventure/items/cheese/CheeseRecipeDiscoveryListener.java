
package ca.itwillbeapps.minecraft.cathysadventure.items.cheese;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;

public class CheeseRecipeDiscoveryListener implements Listener {
    private final CathysAdventure plugin;

    public CheeseRecipeDiscoveryListener(CathysAdventure plugin) {
        this.plugin = plugin;
    }

    /**
     * Listens to item pick-ups to discover the cheese recipe when milk is
     * picked up.
     *
     * @param event The pick-up event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (event.getItem().getItemStack().getType() != Material.MILK_BUCKET) {
            return;
        }

        discoverRecipes(player);
    }

    /**
     * Listens to cow interaction events to discover the cheese recipe.
     *
     * @param event The interaction event
     */
    // Covers milking a cow directly.
    @EventHandler(ignoreCancelled = true)
    public void onCowInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Cow)) {
            return;
        }

        Player player = event.getPlayer();

        ItemStack heldItem = event.getHand() == EquipmentSlot.HAND
            ? player.getInventory().getItemInMainHand()
            : player.getInventory().getItemInOffHand();

        if (heldItem.getType() != Material.BUCKET) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && player.getInventory().contains(Material.MILK_BUCKET)) {
                discoverRecipes(player);
            }
        });
    }

    /**
     * Makes the given player discover the cheese recipes.
     *
     * @param player The player who should discover the recipes
     */
    private void discoverRecipes(Player player) {
        player.discoverRecipe(plugin.getKeys().cheeseFurnaceRecipeIdKey);
        player.discoverRecipe(plugin.getKeys().cheeseSmokerRecipeIdKey);
        player.discoverRecipe(plugin.getKeys().cheeseCampfireRecipeIdKey);
    }
}
