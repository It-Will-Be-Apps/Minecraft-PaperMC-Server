package ca.itwillbeapps.minecraft.cathysadventure.companion.everest;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import ca.itwillbeapps.minecraft.cathysadventure.CathysAdventure;
import ca.itwillbeapps.minecraft.cathysadventure.player.PlayerManager;

public class EverestEnrageListener implements Listener {
    private final CathysAdventure plugin;
    private final PlayerManager playerManager;
    private final EverestEntity everestEntity;
    private final EverestCombat everestCombat;

    public EverestEnrageListener(CathysAdventure plugin, PlayerManager playerManager, EverestEntity everestEntity, EverestCombat everestCombat) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.everestEntity = everestEntity;
        this.everestCombat = everestCombat;
    }

    /**
     * Listens to player deaths to enrage Everest when Cathy dies.
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

        if (everestEntity.isEverest(mob)) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> everestCombat.startEnrage(mob));
    }
}
