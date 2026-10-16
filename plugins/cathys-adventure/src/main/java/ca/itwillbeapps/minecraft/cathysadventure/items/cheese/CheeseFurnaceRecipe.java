package ca.itwillbeapps.minecraft.cathysadventure.items.cheese;

import org.bukkit.Material;
import org.bukkit.inventory.FurnaceRecipe;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.Settings;

public class CheeseFurnaceRecipe {
    private final CathysAdventure plugin;
    private final CheeseItem cheeseItem;

    public CheeseFurnaceRecipe(CathysAdventure plugin, CheeseItem cheeseItem) {
        this.plugin = plugin;
        this.cheeseItem = cheeseItem;
    }

    /**
     * Registers the recipe.
     */
    public void register() {
        FurnaceRecipe recipe = new FurnaceRecipe(
            plugin.getKeys().cheeseFurnaceRecipeIdKey,
            cheeseItem.create(),
            Material.MILK_BUCKET,
            Settings.CHEESE_COOKING_EXPERIENCE,
            Settings.CHEESE_FURNACE_COOKING_TIME_TICKS
        );

        plugin.getServer().addRecipe(recipe);

        plugin.getLogger().info("Registered cheese furnace recipe");
    }

    /**
     * Unregisters the recipe.
     */
    public void unregister() {
        plugin.getServer().removeRecipe(plugin.getKeys().cheeseFurnaceRecipeIdKey);

        plugin.getLogger().info("Unregistered cheese furnace recipe");
    }
}
