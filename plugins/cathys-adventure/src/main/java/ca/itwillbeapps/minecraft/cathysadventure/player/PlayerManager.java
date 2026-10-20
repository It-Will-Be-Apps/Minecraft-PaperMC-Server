
package ca.itwillbeapps.minecraft.cathysadventure.player;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.config.Configurations;

public class PlayerManager {
    private final CathysAdventure plugin;

    private Player cathy;
    private PlayerListener playerListener;
    private PlayerDeathListener playerDeathListener;

    public PlayerManager(CathysAdventure plugin) {
        this.plugin = plugin;
    }

    /**
     * Finds the Cathy player and initializes the player listener.
     */
    public void initialize() {
        cathy = findCathy();

        PluginManager pluginManager = plugin.getServer().getPluginManager();

        playerListener = new PlayerListener(this);
        pluginManager.registerEvents(playerListener, plugin);

        playerDeathListener = new PlayerDeathListener(plugin);
        pluginManager.registerEvents(playerDeathListener, plugin);

        plugin.getLogger().info("Player manager initialized");
    }

    /**
     * Browses logged in players to find Cathy.
     *
     * @return Cathy if she is online, null otherwise
     */
    private Player findCathy() {
        String cathyUsername = getCathyUsername();

        if (cathyUsername.isBlank()) {
            return null;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(cathyUsername)) {
                return player;
            }
        }

        return null;
    }

    /**
     * Returns Cathy's username.
     *
     * @return Cathy's username, or an empty string if it is not defined
     */
    public String getCathyUsername() {
        return plugin.getConfig().getString(Configurations.CATHY_USERNAME);
    }

    /**
     * Returns Cathy if she is logged in.
     *
     * @return Cathy, or null if she isn't logged in
     */
    public Player getCathy() {
        return cathy;
    } 
   
    /**
     * Sets the Cathy player
     *
     * @param player The Cathy player
     */
    public void setCathy(Player player) {
        cathy = player;
    }

    /**
     * Cleans up the player listeners.
     */
    public void cleanUp() {
        HandlerList.unregisterAll(playerListener);
        HandlerList.unregisterAll(playerDeathListener);

        plugin.getLogger().info("Everest manager disabled");
    }
}
