package ca.itwillbeapps.minecraft.cathysadventure.items.cheese;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.SmokingRecipe;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;

public class CheeseSmokerRecipe {
    private static final String RECIPE_ID = "CHEESE_SMOKER_RECIPE";
    private static final int COOKING_TIME_TICKS = 100;
    private static final float EXPERIENCE = 0.0f;

    private final CathysAdventure plugin;
    private final CheeseItem cheeseItem;
    private final NamespacedKey recipeKey;

    public CheeseSmokerRecipe(CathysAdventure plugin, CheeseItem cheeseItem) {
        this.plugin = plugin;
        this.cheeseItem = cheeseItem;
        this.recipeKey = new NamespacedKey(plugin, RECIPE_ID);
    }

    /**
     * Registers the recipe.
     */
    public void register() {
        SmokingRecipe recipe = new SmokingRecipe(
            recipeKey,
            cheeseItem.create(),
            Material.MILK_BUCKET,
            EXPERIENCE,
            COOKING_TIME_TICKS
        );

        plugin.getServer().addRecipe(recipe);

        plugin.getLogger().info("Registered cheese smoker recipe.");
    }

    /**
     * Unregisters the recipe.
     */
    public void unregister() {
        plugin.getServer().removeRecipe(recipeKey);
    }
}
