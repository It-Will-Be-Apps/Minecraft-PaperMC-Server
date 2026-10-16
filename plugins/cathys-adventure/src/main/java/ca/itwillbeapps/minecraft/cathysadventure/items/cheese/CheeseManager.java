package ca.itwillbeapps.minecraft.cathysadventure.items.cheese;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;

public class CheeseManager {
    private final CathysAdventure plugin;

    private CheeseItem cheeseItem;
    private CheeseFurnaceRecipe cheeseFurnaceRecipe;
    private CheeseSmokerRecipe cheeseSmokerRecipe;
    private CheeseCampfireRecipe cheeseCampfireRecipe;
    private CheeseCookingListener cheeseCookingListener;
    private CheeseConsumptionListener cheeseConsumptionListener;
    private CheeseRecipeDiscoveryListener cheeseRecipeDiscoveryListener;
    private CheeseCraftingListener cheeseCraftingListener;

    public CheeseManager(CathysAdventure plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes the cheese item, recipes and listeners.
     */
    public void initialize() {
        cheeseItem = new CheeseItem(plugin);

        cheeseFurnaceRecipe = new CheeseFurnaceRecipe(plugin, cheeseItem);
        cheeseFurnaceRecipe.register();

        cheeseSmokerRecipe = new CheeseSmokerRecipe(plugin, cheeseItem);
        cheeseSmokerRecipe.register();

        cheeseCampfireRecipe = new CheeseCampfireRecipe(plugin, cheeseItem);
        cheeseCampfireRecipe.register();

        PluginManager pluginManager = plugin.getServer().getPluginManager();

        cheeseCookingListener = new CheeseCookingListener(plugin, cheeseItem);
        pluginManager.registerEvents(cheeseCookingListener, plugin);

        cheeseConsumptionListener = new CheeseConsumptionListener(cheeseItem);
        pluginManager.registerEvents(cheeseConsumptionListener, plugin);

        cheeseRecipeDiscoveryListener = new CheeseRecipeDiscoveryListener(plugin);
        pluginManager.registerEvents(cheeseRecipeDiscoveryListener, plugin);

        cheeseCraftingListener = new CheeseCraftingListener(cheeseItem);
        pluginManager.registerEvents(cheeseCraftingListener, plugin);

        plugin.getLogger().info("Cheese manager initialized");
    }

    /**
     * Returns the cheese item definition.
     *
     * @return The cheese item definition
     */
    public CheeseItem getCheeseItem() {
        return cheeseItem;
    }

    /**
     * Cleans up the cheese recipes and listeners.
     */
    public void cleanUp() {
        cheeseFurnaceRecipe.unregister();
        cheeseSmokerRecipe.unregister();
        cheeseCampfireRecipe.unregister();

        HandlerList.unregisterAll(cheeseCookingListener);
        HandlerList.unregisterAll(cheeseConsumptionListener);
        HandlerList.unregisterAll(cheeseRecipeDiscoveryListener);
        HandlerList.unregisterAll(cheeseCraftingListener);

        plugin.getLogger().info("Cheese manager disabled");
    }
}
