package ca.itwillbeapps.minecraft.cathysadventure.items.maps;

import java.util.List;

import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.config.Configurations;

public class MapsManager {
    private final CathysAdventure plugin;

    private ObelixMapItem obelixMapItem;
    private GroguMapItem groguMapItem;
    private MapInitializerListener mapInitializerListener;

    public MapsManager(CathysAdventure plugin) {
        this.plugin = plugin;
    }

    /**
     * Initializes the map items and listeners.
     */
    public void initialize() {
        obelixMapItem = new ObelixMapItem(plugin, this);

        groguMapItem = new GroguMapItem(plugin, this);

        PluginManager pluginManager = plugin.getServer().getPluginManager();

        mapInitializerListener = new MapInitializerListener(this);
        pluginManager.registerEvents(mapInitializerListener, plugin);

        plugin.getLogger().info("Maps manager initialized");
    }

    /**
     * Returns the Obélix map item definition.
     *
     * @return The Obélix map item definition
     */
    public ObelixMapItem getObelixMapItem() {
        return obelixMapItem;
    }

    /**
     * Returns the Grogu map item definition.
     *
     * @return The Grogu map item definition
     */
    public GroguMapItem getGroguMapItem() {
        return groguMapItem;
    }

    /**
     * Verifies if the given ID is saved in the plugin configuration files.
     *
     * @param id The id of the map to verify
     * @return Whether the given id matches a saved custom map
     */
    public boolean isCustomMap(int id) {
        List<Integer> savedIds = plugin.getConfig().getIntegerList(Configurations.CUSTOM_MAP_IDS);
        return savedIds.contains(id);
    }

    /**
     * Saves the given ID in the plugin configuration files.
     *
     * @param id The id of the map to save
     */
    public void saveCustomMapId(int id) {
        List<Integer> savedIds = plugin.getConfig().getIntegerList(Configurations.CUSTOM_MAP_IDS);

        if (!savedIds.contains(id)) {
            savedIds.add(id);
            plugin.getConfig().set(Configurations.CUSTOM_MAP_IDS, savedIds);
            plugin.saveConfig();
        }
    }

    /**
     * Cleans up the maps listeners.
     */
    public void cleanUp() {
        HandlerList.unregisterAll(mapInitializerListener);

        plugin.getLogger().info("Maps manager disabled");
    }
}
