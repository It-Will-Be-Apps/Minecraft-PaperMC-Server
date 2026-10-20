package ca.itwillbeapps.minecraft.cathysadventure.companion.greatpyrenees;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;

public class GreatPyreneesEnrageListener implements Listener {
    private final CathysAdventure plugin;
    private final PlayerManager playerManager;
    private final GreatPyreneesCombat greatPyreneesCombat;

    public GreatPyreneesEnrageListener(CathysAdventure plugin, PlayerManager playerManager, GreatPyreneesCombat greatPyreneesCombat) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.greatPyreneesCombat = greatPyreneesCombat;
    }

    /**
     * Listens to player deaths to enrage the great pyrenees when Cathy dies.
     *
     * @param event The player death event
     */
    @EventHandler
    public void onCathyDeath(PlayerDeathEvent event) {
        Player cathy = playerManager.getCathy();

        if (cathy == null || !event.getPlayer().getUniqueId().equals(cathy.getUniqueId())) {
            return;
        }

        Entity killer = event.getDamageSource().getCausingEntity();

        if (!(killer instanceof Mob mob)) {
            return;
        }

        if (killer instanceof Wolf) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> greatPyreneesCombat.startEnrage(mob));
    }
}
