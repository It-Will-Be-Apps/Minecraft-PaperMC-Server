package ca.itwillbeapps.minecraft.cathysadventure;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.GreatPyreneesManager;
import ca.itwillbeapps.minecraft.cathysadventure.items.cheese.CheeseManager;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;

public class CathysAdventure extends JavaPlugin implements Listener {
    private Keys keys;

    private PlayerManager playerManager;
    private CheeseManager cheeseManager;
    private GreatPyreneesManager greatPyreneesManager;

    @Override
    public void onEnable() {
        saveResource("config.yml", false);

        keys = new Keys(this);

        playerManager = new PlayerManager(this);
        playerManager.initialize();

        cheeseManager = new CheeseManager(this);
        cheeseManager.initialize();

        greatPyreneesManager = new GreatPyreneesManager(this, playerManager, cheeseManager.getCheeseItem());
        greatPyreneesManager.initialize();

        getLogger().info("Cathy's Adventure enabled");
    }

    @Override
    public void onDisable() {
        playerManager.cleanUp();
        cheeseManager.cleanUp();
        greatPyreneesManager.cleanUp();
    }

    /**
     * Returns all the namespaced keys for the plugin.
     *
     * @return The plugin namespaced keys
     */
    public Keys getKeys() {
        return keys;
    }
}
