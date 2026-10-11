package ca.itwillbeapps.minecraft.cathysadventure.items.cheese;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.CampfireRecipe;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;

public class CheeseCampfireRecipe {
    private static final String RECIPE_ID = "CHEESE_CAMPFIRE_RECIPE";
    private static final int COOKING_TIME_TICKS = 600;
    private static final float EXPERIENCE = 0.0f;

    private final CathysAdventure plugin;
    private final CheeseItem cheeseItem;
    private final NamespacedKey recipeKey;

    public CheeseCampfireRecipe(CathysAdventure plugin, CheeseItem cheeseItem) {
        this.plugin = plugin;
        this.cheeseItem = cheeseItem;
        this.recipeKey = new NamespacedKey(plugin, RECIPE_ID);
    }

    /**
     * Registers the recipe.
     */
    public void register() {
        CampfireRecipe recipe = new CampfireRecipe(
            recipeKey,
            cheeseItem.create(),
            Material.MILK_BUCKET,
            EXPERIENCE,
            COOKING_TIME_TICKS
        );

        plugin.getServer().addRecipe(recipe);

        plugin.getLogger().info("Registered cheese campfire recipe.");
    }

    /**
     * Unregisters the recipe.
     */
    public void unregister() {
        plugin.getServer().removeRecipe(recipeKey);
    }
}
