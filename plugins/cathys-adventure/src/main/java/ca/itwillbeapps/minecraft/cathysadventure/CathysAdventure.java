package ca.itwillbeapps.minecraft.cathysadventure;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees.GreatPyreneesManager;
import ca.itwillbeapps.minecraft.cathysadventure.companion.grogu.GroguManager;
import ca.itwillbeapps.minecraft.cathysadventure.items.cheese.CheeseManager;
import ca.itwillbeapps.minecraft.cathysadventure.items.maps.MapsManager;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;

public class CathysAdventure extends JavaPlugin implements Listener {
    private Keys keys;

    private PlayerManager playerManager;
    private CheeseManager cheeseManager;
    private GroguManager groguManager;
    private GreatPyreneesManager greatPyreneesManager;
    private MapsManager mapsManager;

    @Override
    public void onEnable() {
        saveResource("config.yml", true);

        keys = new Keys(this);

        playerManager = new PlayerManager(this);
        playerManager.initialize();

        cheeseManager = new CheeseManager(this);
        cheeseManager.initialize();

        mapsManager = new MapsManager(this);
        mapsManager.initialize();

        groguManager = new GroguManager(this, playerManager);
        groguManager.initialize();

        greatPyreneesManager = new GreatPyreneesManager(this, playerManager, groguManager, cheeseManager.getCheeseItem(),
            mapsManager.getObelixMapItem(), mapsManager.getGroguMapItem());
        greatPyreneesManager.initialize();

        getLogger().info("Cathy's Adventure enabled");
    }

    @Override
    public void onDisable() {
        playerManager.cleanUp();
        cheeseManager.cleanUp();
        mapsManager.cleanUp();
        groguManager.cleanUp();
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
