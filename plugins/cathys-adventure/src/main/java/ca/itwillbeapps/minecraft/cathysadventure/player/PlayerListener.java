package ca.itwillbeapps.minecraft.cathysadventure.player;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final PlayerManager playerManager;

    public PlayerListener(PlayerManager playerManager) {
        this.playerManager = playerManager;
    }

    /**
     * Listens to players joining to find and cache Cathy.
     *
     * @param event The player join event
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (event.getPlayer().getName().equalsIgnoreCase(playerManager.getCathyUsername())) {
            playerManager.setCathy(player);
        }
    }

    /**
     * Listens to players leaving to remove Cathy from the cache.
     *
     * @param event The player quit event
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player cathy = playerManager.getCathy();

        if (cathy == null || !event.getPlayer().getUniqueId().equals(cathy.getUniqueId())) {
            return;
        }

        playerManager.setCathy(null);
    }
}
