package ca.itwillbeapps.minecraft.cathysadventure;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class CathysAdventurePlugin extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getLogger().info("Cathy's Adventure has started!");

        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Cathy's Adventure has stopped!");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().sendPlainMessage("Welcome to Cathy's Adventure, " + event.getPlayer().getName() + "!");
    }
}
