package ca.itwillbeapps.minecraft.cathysadventure.items.cheese;

import org.bukkit.Material;
import org.bukkit.inventory.SmokingRecipe;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.Settings;

public class CheeseSmokerRecipe {
    private final CathysAdventure plugin;
    private final CheeseItem cheeseItem;

    public CheeseSmokerRecipe(CathysAdventure plugin, CheeseItem cheeseItem) {
        this.plugin = plugin;
        this.cheeseItem = cheeseItem;
    }

    /**
     * Registers the recipe.
     */
    public void register() {
        SmokingRecipe recipe = new SmokingRecipe(
            plugin.getKeys().cheeseSmokerRecipeIdKey,
            cheeseItem.create(),
            Material.MILK_BUCKET,
            Settings.CHEESE_COOKING_EXPERIENCE,
            Settings.CHEESE_SMOKER_COOKING_TIME_TICKS
        );

        plugin.getServer().addRecipe(recipe);

        plugin.getLogger().info("Registered cheese smoker recipe");
    }

    /**
     * Unregisters the recipe.
     */
    public void unregister() {
        plugin.getServer().removeRecipe(plugin.getKeys().cheeseSmokerRecipeIdKey);

        plugin.getLogger().info("Unregistered cheese smoker recipe");
    }
}
