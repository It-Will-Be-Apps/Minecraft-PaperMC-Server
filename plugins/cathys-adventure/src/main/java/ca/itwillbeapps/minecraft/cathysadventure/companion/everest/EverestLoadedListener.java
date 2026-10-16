package ca.itwillbeapps.minecraft.cathysadventure.companion.everest;

import java.util.UUID;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

public class EverestLoadedListener implements Listener {
    private final EverestManager everestManager;
    private final EverestEntity everestEntity;

    public EverestLoadedListener(EverestManager everestManager, EverestEntity everestEntity) {
        this.everestManager = everestManager;
        this.everestEntity = everestEntity;
    }

    /**
     * Listens to chunks loading to find Everest.
     *
     * @param event The chunk loading event
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        UUID everestUuid = everestManager.getEverestUuid();

        for (Entity entity : event.getChunk().getEntities()) {
            if (!(entity instanceof Wolf wolf) || wolf.isDead()) {
                continue;
            }

            if (everestUuid != null && everestUuid.equals(wolf.getUniqueId())) {
                everestEntity.markAsEverest(wolf);
                everestManager.setEverest(wolf);
                return;
            }
        }
    }
}
