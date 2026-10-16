package ca.itwillbeapps.minecraft.cathysadventure.items.cheese;

import org.bukkit.Material;
import org.bukkit.inventory.CampfireRecipe;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.Settings;

public class CheeseCampfireRecipe {
    private final CathysAdventure plugin;
    private final CheeseItem cheeseItem;

    public CheeseCampfireRecipe(CathysAdventure plugin, CheeseItem cheeseItem) {
        this.plugin = plugin;
        this.cheeseItem = cheeseItem;
    }

    /**
     * Registers the recipe.
     */
    public void register() {
        CampfireRecipe recipe = new CampfireRecipe(
            plugin.getKeys().cheeseCampfireRecipeIdKey,
            cheeseItem.create(),
            Material.MILK_BUCKET,
            Settings.CHEESE_COOKING_EXPERIENCE,
            Settings.CHEESE_CAMPFIRE_COOKING_TIME_TICKS
        );

        plugin.getServer().addRecipe(recipe);

        plugin.getLogger().info("Registered cheese campfire recipe");
    }

    /**
     * Unregisters the recipe.
     */
    public void unregister() {
        plugin.getServer().removeRecipe(plugin.getKeys().cheeseCampfireRecipeIdKey);

        plugin.getLogger().info("Unregistered cheese campfire recipe");
    }
}
