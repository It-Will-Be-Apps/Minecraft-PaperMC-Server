package ca.itwillbeapps.minecraft.cathysadventure.companion.everest;

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

public class EverestEnrageListener implements Listener {
    private final CathysAdventure plugin;
    private final PlayerManager playerManager;
    private final EverestManager everestManager;
    private final EverestEntity everestEntity;
    private final EverestCombat everestCombat;

    public EverestEnrageListener(CathysAdventure plugin, PlayerManager playerManager, EverestManager everestManager, EverestEntity everestEntity, EverestCombat everestCombat) {
        this.plugin = plugin;
        this.playerManager = playerManager;
        this.everestManager = everestManager;
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
        Wolf everest = everestManager.getEverest();

        if (everest == null || everest.isDead() || !everest.isValid() || !everest.isTamed()) {
            return;
        }

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
